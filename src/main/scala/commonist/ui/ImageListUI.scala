package commonist.ui

import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Image
import java.awt.Rectangle
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.io.File

import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.ScrollPaneConstants

import commonist.Constants
import commonist.data.ImageData
import commonist.data.ImageListData
import commonist.util.Messages
import commonist.util.TextUtil2

import scutil.gui.GridBagDSL._
import scutil.gui.CasterInstances._

/** displays a scrollable List of ImageUIs */
final class ImageListUI(programHeading:String, programIcon:Image) extends JPanel { outer =>
	private val imageUIs	= new scala.collection.mutable.ListBuffer[ImageUI]
	
	//------------------------------------------------------------------------------
	//## components

	private val listPanel	= new ListPanel()
	listPanel setLayout new BoxLayout(listPanel, BoxLayout.Y_AXIS)
	
	private val scroll	= new JScrollPane(listPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
	scroll setBorder (BorderFactory createEmptyBorder (0,0,0,0))	//### scrollBorder?
	
	//var	bar	= scroll.getVerticalScrollBar

	private val selectLabel			= new JLabel(Messages text "imageList.select")
	private val	selectAllButton		= new JButton(Messages text "imageList.select.all")
	private val	selectNoneButton	= new JButton(Messages text "imageList.select.none")
	private val	selectFailedButton	= new JButton(Messages text "imageList.select.failed")
	private val selectStatus		= new JLabel
	
	//------------------------------------------------------------------------------
	//## layout
	
	setBorder(Constants.PANEL_BORDER)

	setLayout(new GridBagLayout)
	add(selectLabel,		GBC pos(0,0) size(1,1) weight(0.001,0)	anchor CENTER	fill HORIZONTAL insets (0,0,0,2))
	add(selectNoneButton,	GBC pos(1,0) size(1,1) weight(0.001,0)	anchor CENTER	fill HORIZONTAL	insets (0,0,0,2))
	add(selectAllButton,	GBC pos(2,0) size(1,1) weight(0.001,0)	anchor CENTER	fill HORIZONTAL	insets (0,2,0,2))
	add(selectFailedButton,	GBC pos(3,0) size(1,1) weight(0.001,0)	anchor CENTER	fill HORIZONTAL	insets (0,2,0,0))
	add(selectStatus,		GBC pos(4,0) size(1,1) weight(1,0)		anchor CENTER	fill HORIZONTAL	insets (0,8,0,4))
	add(scroll,				GBC pos(0,1) size(5,1) weight(1,1)		anchor CENTER	fill BOTH		insets (4,0,0,0))

	//------------------------------------------------------------------------------
	//## wiring
	
	selectAllButton onActionPerformed { _ =>
		selectAll() 
	}
	selectNoneButton onActionPerformed { _ =>
		selectNone() 
	}
	selectFailedButton onActionPerformed { _ =>
		selectFailed() 
	}
	
	//------------------------------------------------------------------------------
	//## init
	
	updateSelectStatus()
	
	/** removes all ImageUI objects */
	def clear() {
		imageUIs.clear()
		listPanel.removeAll()
	}
	
	/** adds a File UI */
	def add(file:File, icon:Option[Icon], thumbnailMaxSize:Int) {
		val	imageUI	= new ImageUI(file, icon, thumbnailMaxSize, programHeading, programIcon, new ImageUICallback {
			def updateSelectStatus() {  outer.updateSelectStatus() }
		})
		
		imageUIs += imageUI
		listPanel.add(imageUI)
	}
	
	/** get the select status and update the display */
	def updateSelectStatus() {
		val allFiles		= imageUIs									map { _.getData.file }
		val selectedFiles	= imageUIs filter { _.isUploadSelected }	map { _.getData.file }
		val allBytes		= (allFiles			map { _.length }).foldLeft (0L)(_+_)
		val selectedBytes	= (selectedFiles	map { _.length }).foldLeft (0L)(_+_)
	
		selectStatus.setText(Messages.message("imageList.selected",
			int2Integer(selectedFiles.size), int2Integer(allFiles.size), TextUtil2.human(selectedBytes), TextUtil2.human(allBytes) 
		))
	}
	
	def getData:ImageListData = new ImageListData(imageUIs.toList map { _.getData })
	
	/** set the upload state for the ImageUI representing the given file */
	def uploadFinished(file:File, success:Boolean) {
		imageUIs filter { _.getData.file == file } foreach { _ setUploadSuccessful Some(success) }
	}
	
	//------------------------------------------------------------------------------
	//## private methods
	
	/** checks the upload checkbox in all images */
	private def selectAll() {
		imageUIs foreach { _ setUploadSelected true }
		updateSelectStatus()
	}
	
	/** unchecks the upload checkbox in all images */
	private def selectNone() {
		imageUIs foreach { _ setUploadSelected false }
		updateSelectStatus()
	}
	
	/** checks for the upload checkbox in all failed images, unchecks it for the rest */
	private def selectFailed() {
		imageUIs foreach { it => it setUploadSelected (it.getUploadSuccessful == Some(false)) }
		updateSelectStatus()
	}
	
	//------------------------------------------------------------------------------
	//## private classes
	
	/** a Scrollable Panel scrolling to even tickets */
	private class ListPanel extends JPanel with Scrollable {
		/**	visibleRect	The view area visible within the viewport
			orientation	SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
			direction	Less than zero to scroll up/left, greater than zero for down/right.
		*/
		def getScrollableUnitIncrement(visibleRect:Rectangle, orientation:Int, direction:Int):Int = {
			if (orientation == SwingConstants.HORIZONTAL)	return 1
			
			if (direction < 0) {
				val component	= getComponentAt(visibleRect.x, visibleRect.y - 2)
				val	visible		= visibleRect.y
				val	target		= if (component != null) component.getY else 0
				visible - target
			}
			else {
				val component	= getComponentAt(visibleRect.x, visibleRect.y + visibleRect.height)
				val	visible		= visibleRect.y + visibleRect.height
				val	target		= if (component != null) (component.getY + component.getHeight) else this.getHeight
				target - visible
			}
		}
		
		/**	visibleRect	The view area visible within the viewport
			orientation	SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
			direction	Less than zero to scroll up/left, greater than zero for down/right.
		*/
		def getScrollableBlockIncrement(visibleRect:Rectangle, orientation:Int, direction:Int):Int = {
			if (orientation == SwingConstants.HORIZONTAL)	return 1
			
			if (direction < 0)	visibleRect.height min (visibleRect.y - 0)
			else				visibleRect.height min (this.getHeight  - (visibleRect.y + visibleRect.height))
		}  
		
		def getPreferredScrollableViewportSize():Dimension	= this.getPreferredSize
		def getScrollableTracksViewportWidth():Boolean		= true
		def getScrollableTracksViewportHeight():Boolean		= false
	}
}
