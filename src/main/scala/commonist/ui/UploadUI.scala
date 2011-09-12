package commonist.ui

import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.event.ActionListener
import java.awt.event.ActionEvent

import javax.swing.JButton
import javax.swing.JPanel

import scutil.gui.GridBagDSL._
import scutil.gui.CasterInstances._

import commonist.util.Messages

/** action events this UI sends */
trait UploadUICallback {
	def startUpload()
	def stopUpload()
}

final class UploadUI(callback:UploadUICallback) extends JPanel {
	private val uploadButton	= new JButton(Messages text "upload.upload")
	private val abortButton		= new JButton(Messages text "upload.abort")
	
	setLayout(new GridBagLayout)
	add(abortButton,	GBC pos (0,0) size (1,1) weight (1,0) fill HORIZONTAL)
	add(uploadButton,	GBC pos (1,0) size (1,1) weight (1,0) fill HORIZONTAL)
	
	uploadButton onActionPerformed { _ => 
		callback.startUpload() 
	}
	abortButton onActionPerformed { _ => 
		callback.stopUpload() 
	}
}
