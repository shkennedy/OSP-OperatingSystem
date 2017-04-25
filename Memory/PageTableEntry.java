// Shane Kennedy
// 110066543
// I pledge my honor that all parts of this project were done by me individually
// and without collaboration with anybody else.

package osp.Memory;

import java.util.Date;
import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    private static Date date = new Date();
    private long lastReferenceTime;
    
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        super(ownerPageTable, pageNumber);
    }

    /**
        This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {   
        // Check page validity
        if (!isValid()) {
            ThreadCB lockingThread = iorb.getThread();
            ThreadCB validatingThread = getValidatingThread();
            
            // First thread to request page, create pageFault event
            if (validatingThread == null) {
                int rt = PageFaultHandler.handlePageFault(iorb.getThread(), 
                                                          MemoryLock, 
                                                          this);
                // Update page validity if pageFault was resolved
                if (rt == SUCCESS) {
                    setValid(true);
                }
            }
            
            // New thread requesting invalid page
            else if (!validatingThread.equals(lockingThread)) {
                // Suspend new thread until pageFault is completed
                lockingThread.suspend(this);
                
                // Check for resolution of pageFault
                if (!isValid()) {
                    // Page is still invalid
                    return FAILURE;
                }
            }
        }
     
        // Increment lockCount on associted frame
        if (getFrame() == null) {
            System.out.println("what: " + toString() + " " + isValid());
            return FAILURE;
        }
        getFrame().incrementLockCount();
        
        return SUCCESS;
    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
        // Decrement lock counter on associated frame
        getFrame().decrementLockCount();
    }
    
    /**
    
    */
    public void updateLastReferenceTime() {
        lastReferenceTime = date.getTime();
    }
    
    /**
    
    */
    public long getLastReferenceTime() {
        return lastReferenceTime;
    }
}