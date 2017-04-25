// Shane Kennedy
// 110066543

package osp.Threads;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import osp.Devices.Device;
import osp.IFLModules.*;
import osp.Memory.MMU;
import osp.Memory.PageTable;
import osp.Resources.ResourceCB;
import osp.Tasks.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
    static Comparator<ThreadCB> threadComp = (ThreadCB a, ThreadCB b)->
            a.getTimeOnCPU() > b.getTimeOnCPU()? 1 : -1;
    static PriorityQueue<ThreadCB> readyQueue = new PriorityQueue<ThreadCB>(threadComp);
    
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
        super();
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here
        
    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        // Check for thread limit
        if (task.getThreadCount() == MaxThreadsPerTask) {
            dispatch();
            return null;
        }
        
        // Insert newThread into threadQueue
        ThreadCB newThread = new ThreadCB();
        newThread.setStatus(ThreadReady);
        newThread.setTask(task);
        task.addThread(newThread);
        readyQueue.add(newThread);
        
        // Dispatch new thread
        dispatch();
        
        return newThread;
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        // Update thread status
        int prevStatus = getStatus();
        setStatus(ThreadKill);
        
        // Update task, kill if no threads remain
        TaskCB task = getTask();
        task.removeThread(this);
        if (task.getThreadCount() == 0) {
            task.kill();
        }
        
        // remove from readyQueue if was enqueued
        if (prevStatus == ThreadReady) {
            readyQueue.remove(this);
        }
        
        // Null PTBR if thread was running
        if (prevStatus == ThreadRunning) {
            task.setCurrentThread(null);
            MMU.setPTBR(null);
        }
        
        // Purge thread IORBs if thread is waiting (at any level)
        if (prevStatus >= ThreadWaiting)
            for (int i = 0; i < Device.getTableSize(); ++i) {
                Device.get(i).cancelPendingIO(this);
            }
        
        // Restore resources used by thread
        ResourceCB.giveupResources(this);
        
        // Dispatch new thread
        dispatch();
    }

    /** Suspends the thread that is currently on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        int ts = this.getStatus();
        // Set status to ThreadWaiting if prev ThreadRunning or ThreadWaiting
        if (ts <= ThreadRunning)
            this.setStatus(ThreadWaiting);
        // Increase level of waiting
        else
            this.setStatus(ts + 1);
        
        // Update thread if it was running
        if (ts == ThreadRunning) {
            this.getTask().setCurrentThread(null);
            MMU.setPTBR(null);
        }
        
        // Add thread to waiting queue
        event.addThread(this);
            
        // Dispatch new thread
        dispatch();
    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        // Check for bad cases
        int status = this.getStatus();
        if (status <= ThreadRunning) {
            System.out.println("Attemped to resume non waiting thread: " + this);
            return;
        }
        
        // Move status down one level (closer to ready)
        if (status == ThreadWaiting) {
            this.setStatus(ThreadReady);
            readyQueue.add(this);
        } else if (status > ThreadWaiting) {
            this.setStatus(status - 1);
        }
        
        // Dispatch new thread
        dispatch();
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one thread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
        // Update state of prev running thread
        PageTable pt = MMU.getPTBR();
        if (pt != null) {
            ThreadCB prevThread = pt.getTask().getCurrentThread();
            prevThread.setStatus(ThreadReady);
            prevThread.getTask().setCurrentThread(null);
            MMU.setPTBR(null);
            readyQueue.add(prevThread);
        }
        
        // Retrieve next thread in readyQueue
        ThreadCB nextThread = readyQueue.poll();
        
        // Check if readyQueue is empty -> reschedule prev thread
        if (nextThread == null) {
            return FAILURE;
        }
        
        // Update state of scheduled thread
        nextThread.setStatus(ThreadRunning);
        MMU.setPTBR(nextThread.getTask().getPageTable());
        nextThread.getTask().setCurrentThread(nextThread);
        
        return SUCCESS;
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        Iterator<ThreadCB> it = readyQueue.iterator();
        ThreadCB cursor;

        while (it.hasNext()) {
            cursor = it.next();
            System.out.println("thread: " + cursor.toString());
        }
    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
//        Iterator<ThreadCB> it = readyQueue.iterator();
//        ThreadCB cursor;
//        while ((cursor = it.next()) != null) {
//            // Print some stuff
//        }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
