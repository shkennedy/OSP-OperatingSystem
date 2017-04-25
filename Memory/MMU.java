// Shane Kennedy
// 110066543
// I pledge my honor that all parts of this project were done by me individually
// and without collaboration with anybody else.

package osp.Memory;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
        // Initialize frame table
        int frameTableSize = MMU.getFrameTableSize();
        for (int i = 0; i < frameTableSize; ++i) {
            setFrame(i, new FrameTableEntry(i));
        }
    }

    /**
       This method handles memory references. The method must 
       calculate which memory page contains the memoryAddress,
       determine whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, 
                                          ThreadCB thread)
    {         
        // Find page to which memoryAddress belongs
        int pageNumber = memoryAddress >> 
                (getVirtualAddressBits() - getPageAddressBits());
        PageTableEntry page = thread.getTask().getPageTable().pages[pageNumber];
                
        // Miss, invalid page
        if (!page.isValid()) {
            
            // Preexisting pagefault on page, another thread of task validating
            if (page.getValidatingThread() != null) {
                
                // Suspend thread to wait for page
                thread.suspend(page);
            }
            
            // Need to start pagefault on page, thread becomes validating thread 
            else {
                
                // Create interrupt event for pagefault
                InterruptVector.setInterruptType(PageFault);
                InterruptVector.setThread(thread);
                InterruptVector.setPage(page);
                InterruptVector.setReferenceType(referenceType);
                
                // Start pagefault
                CPU.interrupt(PageFault);
            }
            
        }
        
        // If thread was killed, exit
        if (thread.getStatus() == ThreadKill) {
            return page;
        }
        
        // Get frame holding page
        FrameTableEntry frame = page.getFrame();
        
        // Update frame to indicate reference
        if (referenceType == MemoryWrite) {
            frame.setDirty(true);
        }
        if (!frame.getPage().isValid()) {
            System.out.println("FUCKER: " + frame.toString() + ", " + frame.getPage().toString());
        }
        frame.setReferenced(true);
        
        // Update page's lastReferenceTime
        page.updateLastReferenceTime();
        
        return page;
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}