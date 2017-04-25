// Shane Kennedy
// 110066543
// I pledge my honor that all parts of this project were done by me individually
// and without collaboration with anybody else.

package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        super(ownerTask);
        
        int numPages = (int)Math.pow(2, MMU.getPageAddressBits());
        pages = new PageTableEntry[numPages];
        for (int i = 0; i < pages.length; ++i) {
            pages[i] = new PageTableEntry(this, i);
        }
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        FrameTableEntry frame;
        TaskCB task;
        for (PageTableEntry page : pages) {
            task = page.getTask();
            
            // Clear frame
            frame = page.getFrame();
            if (frame != null) {
                frame.setPage(null);
                frame.setDirty(false);
                frame.setReferenced(false);
                
            
                // Unreserve frame reserved by current page's task
                if (task.equals(frame.getReserved())) {
                    frame.setUnreserved(task);
                }            
            }
        }    
    }
}