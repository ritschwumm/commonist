package commonist

import scutil.gui.SwingUtil.edt
import scutil.platform.ExceptionUtil
import scutil.log._

object Commonist extends Logging {
	def main(args:Array[String]) {
		ExceptionUtil logAllExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
		ExceptionUtil logAWTExceptions { (thread,throwable) =>
			ERROR("unexpected error", thread, throwable)
		}
	
		edt {
			try {
				CommonistMain
			}
			catch { case e:Exception =>
				ERROR("cannot start application", e)
			}
		}
	}
}
