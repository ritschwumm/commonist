package commonist.ui

import javax.swing.JProgressBar
import javax.swing.SwingConstants

import commonist.util.Messages

/** a JProgressBar displaying Messages */
final class StatusUI extends JProgressBar(SwingConstants.HORIZONTAL) {
	setStringPainted(true)
	
	/** changes the upload progressbar to indeterminate state */
	def indeterminate(messageKey:String, data:Object*) {
		setIndeterminate(true)
		setString(Messages.message(messageKey, data:_*))
	}
	
	/** changes the upload progressbar to determinate state */
	def determinate(messageKey:String, value:Int, maximum:Int, data:Object*) {
		setIndeterminate(false)
		setString(Messages.message(messageKey, data:_*))
		setMaximum(maximum)
		setValue(value)
	}
	
	/** changes the upload progressbar to determinate state */
	def halt(messageKey:String, data:Object*) {
		setIndeterminate(false)
		setString(Messages.message(messageKey, data:_*))
		setMaximum(0)
		setValue(0)
	}
}
