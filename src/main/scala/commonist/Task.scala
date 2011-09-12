package commonist

import scutil.log.Logging

final class TaskVar[T <: Task] {
	private var	current:Option[T]	= None
	def change(newTask:T) {
		val	old	= current
		current	= Some(newTask)
		old match {
			case Some(task)	=> task replace newTask
			case None		=> newTask.start()
		}
	}
	def abort() {
		current foreach { _.abort() }
	}
}

/** base class for UI tasks */
abstract class Task extends Logging { outer =>
	class AbortedException extends Exception
	
	private var alive:Boolean	= false
	private var aborted:Boolean	= false
	
	private val thread:Thread = new Thread(
		new Runnable {
			def run() {
				outer.run()
			}
		}
	)
	// see http://java.sun.com/developer/JDCTechTips/2005/tt0727.html#1
	thread.setPriority(Thread.NORM_PRIORITY)
	private var waitFor:Thread	= null
	
	/** start the task in its own thread */
	def start():Unit = synchronized {
		aborted	= false
		alive	= true
		thread.start()
	}
	
	/** request the task to abort */
	def abort():Unit = synchronized {
		if (!alive)	return
		aborted	= true
	}
	
	/** start another task and request this one to stop */
	def replace(task:Task):Unit = synchronized {
		task.start(thread)
		abort()
	}
	
	/** for use within the task */
	protected def check():Unit = synchronized {
		if (aborted)	throw new AbortedException
	}
	
	/** this method does the real work, implement in subclasses */
	protected def execute()
	
	/** optionally waits until another Task has finished and start this Task */
	private def start(waitFor:Thread):Unit = synchronized {
		this.waitFor	= waitFor
		start()
	}
	
	/** executes this Task, but waits until a Task to be replaced is finished before */
	private def run() {
		if (maybeWaitForReplaced()) {
			execute()
		}
		alive	= false
	}
	
	/** waits until a Task to be replaced is finished, if there is such a Task */
	private def maybeWaitForReplaced():Boolean = {
		if (waitFor == null)	return true
		
		try {
			waitFor.join()
			true
		}
		catch { case e:InterruptedException => 
			ERROR("task interrupted", e)
			false
		}
	}
}
