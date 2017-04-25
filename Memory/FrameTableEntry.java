// Shane Kennedy
// 110066543
// I pledge my honor that all parts of this project were done by me individually
// and without collaboration with anybody else.

package osp.Memory;

/**
    The FrameTableEntry class contains information about a specific page
    frame of memory.

    @OSPProject Memory
*/
import osp.Tasks.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.IflFrameTableEntry;

public class FrameTableEntry extends IflFrameTableEntry
{
    /**
       The frame constructor. Must have

       	   super(frameID)
	   
       as its first statement.

       @OSPProject Memory
    */
    public FrameTableEntry(int frameID)
    {
        super(frameID);
    }

}

/*
      Feel free to add local classes to improve the readability of your code
*/
