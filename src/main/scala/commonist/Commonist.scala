package commonist

import scutil.Concurrent
import scutil.log.Logging
import scutil.gui.SwingUtil._

object Commonist extends Logging {
	/** main entry point */
	def main(args:Array[String]) {
		Concurrent installDefaultUncaughtExceptionHandler  { (t,e) => ERROR("Exception caught in thread: " + t.getName, e)  }
		edt {
			try { new CommonistMain().init() }
			catch { case e:Exception	=> ERROR("cannot start program", e) }
		}
	}
}
