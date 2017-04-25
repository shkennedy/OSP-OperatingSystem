// Shane Kennedy
// 110066543

package osp.Threads;

import osp.IFLModules.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**    
       The timer interrupt handler.  This class is called upon to
       handle timer interrupts.

       @OSPProject Threads
*/
public class TimerInterruptHandler extends IflTimerInterruptHandler
{
    private static final int ticksPerSlice = 100;
    /**
       This basically only needs to reset the times and dispatch
       another process.

       @OSPProject Threads
    */
    public void do_handleInterrupt()
    {        
        System.out.println("PREEMT");
        // Dispatch another process
        ThreadCB.dispatch();
        
        // Reset timer for next cycle
        HTimer.set(ticksPerSlice);

    }

    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
