package commonist.ui.later

import scutil.gui.SwingUtil._

import commonist.ui.StatusUI

/** wraps a StatusUI's methods in SwingUtilities.invokeAndWait */
final class StatusUILater(ui:StatusUI) {
	def indeterminate(messageKey:String, messageArgs:Object*) {
		edtWait {
			ui indeterminate (messageKey, messageArgs:_*)
		}
	}

	def determinate(messageKey:String, cur:Int, max:Int, messageArgs:Object*) {
		edtWait {
			ui determinate (messageKey, cur, max, messageArgs:_*)
		}
	}
	
	def halt(messageKey:String, messageArgs:Object*) {
		edtWait {
			ui halt (messageKey, messageArgs:_*)
		}
	}
}
