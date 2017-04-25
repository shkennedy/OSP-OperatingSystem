// Shane Kennedy
// 110066543
// I pledge my honor that all parts of this project were done by me individually
// and without collaboration with anybody else.

package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {   
        // Set thread as validatingThread of page
        page.setValidatingThread(thread);
        
        // Page is valid
        if (page.isValid()) {
//            ThreadCB.dispatch();
            return FAILURE;
        }
        
        // Find a victim page to evict
        FrameTableEntry frame = null;
        if ((frame = getCandidateFrame(thread.getTask())) == null) {
//            ThreadCB.dispatch();
            return NotEnoughMemory;
        }
        
        // Suspend page on pagefault event
        SystemEvent pageFaultEvent = new SystemEvent("PageFault");
        thread.suspend(pageFaultEvent);
        
        // Swap out candidate page from frame, swap in new page
        if (swapOut(frame, thread) == FAILURE || 
                swapIn(frame, page, thread) == FAILURE) {
//            ThreadCB.dispatch();
            return FAILURE;
        }
        
        // Notify all waiting threads about page's change
        pageFaultEvent.notifyThreads();
        page.notifyThreads();
        
        // Dispatch next thread
        ThreadCB.dispatch();
        
        return SUCCESS;
    }
    
    /*
        
    */
    private static FrameTableEntry getCandidateFrame(TaskCB task) 
    {
        FrameTableEntry candidate = null, frame;
        int nFrames = MMU.getFrameTableSize();
        
        // Find free frame
        for (int i = 0; i < nFrames; ++i) {
            frame = MMU.getFrame(i);
            
            // Ignore locked frames
            if (frame.getLockCount() > 0) {
                continue;
            }
            
            // Free frame, highest priority
            if (frame.getPage() == null && !frame.isReserved()) {
                frame.setReserved(task);
                return frame;
            }
            
            // If Frame is not reserved nor locked, save it as a candidate
            if (!frame.isReserved() && frame.getLockCount() == 0) {
                
                if (candidate != null) { 
                    // If current frame's page was used less recently than 
                    // candidate save it as new candidate                         
                    if (frame.getPage().getLastReferenceTime() < 
                            candidate.getPage().getLastReferenceTime()) {
                        candidate = frame;
                    }
                }
                
                else {
                    candidate = frame;
                }
            }
        }
        
        if (candidate != null) {
            // Candidate is still unreserved, set reserved with task
            if (!candidate.isReserved()) {
                candidate.setReserved(task);
            }
            
            // Candidate is now reserved, call again to find new candidate
            else {
                return getCandidateFrame(task);
            }
        }
        
        return candidate;
    }
    
    /*
    
    */
    private static int swapOut(FrameTableEntry frame, 
                               ThreadCB thread) 
    {
        // Page is already free, return
        if (frame.getPage() == null) {
            return SUCCESS;
        }
        
        // Get previous page
        PageTableEntry prevPage = frame.getPage();
        
        // Page is dirty, swap out
        if (frame.isDirty()) {
            
            // Write contents to swap file
            OpenFile swapFile = prevPage.getTask().getSwapFile();
            swapFile.write(prevPage.getID(), prevPage, thread);
            
            // Check if thread was killed
            if (thread.getStatus() == ThreadKill) {
                return FAILURE;
            }
            
            // Mark frame as clean
            frame.setDirty(false);
        }
        
        // Check if prevPage was purged from frame
        if (frame.getPage() == null) {
            return SUCCESS;
        }
        
        // Update pagetable to invalidate prevPage
        if (frame.getPage().isValid()) {
            frame.setReferenced(false);
        }
        prevPage.setValid(false);           
        prevPage.setFrame(null);
        
        // Free frame
        frame.setPage(null);
        
        return SUCCESS;
    }
    
    /*
    
    */
    private static int swapIn(FrameTableEntry frame, 
                              PageTableEntry page,
                              ThreadCB thread) 
    {   
        // Update frame and page
        frame.setPage(page);
        page.setFrame(frame);
        
        // Swap in new page
        OpenFile swapFile = page.getTask().getSwapFile();
        swapFile.read(page.getID(), page, thread);
        
        // Check if thread was killed
        if (thread.getStatus() == ThreadKill) {
            return FAILURE;
        }
        
        // Mark page as valid
        page.setValid(true);
        
        // Unreserve page since swap in complete
        frame.setUnreserved(thread.getTask());
        
        return SUCCESS;
    }
    
}