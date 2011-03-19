package commonist.ui

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Image
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.WindowConstants

import commonist.util.Settings
import commonist.util.UIUtil2

/** action events this UI sends */
trait MainWindowCallback {
	def quit()
}
	
/** the application window */
final class MainWindow(
			commonUI:CommonUI, directoryUI:DirectoryUI, 
			imageListUI:ImageListUI, statusUI:StatusUI, uploadUI:UploadUI, 
			programHeading:String, programIcon:Image, callback:MainWindowCallback) {
	
	//------------------------------------------------------------------------------
	//## panels
	
	private val commonPanel	= new JPanel()
	commonPanel setLayout	new BorderLayout
	commonPanel add	(commonUI,		BorderLayout.NORTH)
	commonPanel add	(directoryUI,	BorderLayout.CENTER)
	
	private val mainSplit	= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commonPanel, imageListUI)
	mainSplit.setResizeWeight(0)
	
	private val	uploadPanel	= new JPanel()
	uploadPanel setLayout	new BorderLayout
	uploadPanel add	(statusUI,	BorderLayout.CENTER)
	uploadPanel add	(uploadUI,	BorderLayout.EAST)
	
	private val	windowPanel	= new JPanel()
	windowPanel setLayout	new BorderLayout
	windowPanel add	(mainSplit, 	BorderLayout.CENTER)
	windowPanel add	(uploadPanel,	BorderLayout.SOUTH)
	
	//------------------------------------------------------------------------------
	//## frame
	
	val window	= new JFrame(programHeading)
	window setIconImage programIcon
	window.getContentPane add windowPanel
	window.pack()
	window setSize new Dimension(800, 600)
	window setDefaultCloseOperation WindowConstants.DO_NOTHING_ON_CLOSE	// EXIT_ON_CLOSE or DISPOSE_ON_CLOSE
	
	window.setLocationRelativeTo(null)
	
	// quit on window close 
	window addWindowListener new WindowAdapter {
		override def windowClosing(ev:WindowEvent) {
			window.dispose()
			callback.quit()
		}
	}
	
	/** should be called after loadProperties */
	def makeVisible() {
		window.setVisible(true)
	}

	/** call when ImageUIs have been added or removed */
	def revalidate() {
		mainSplit.revalidate()
	}
	
	//------------------------------------------------------------------------------
	//## Settings

	/** loads this UI's state from the properties */
	def loadSettings(settings:Settings) {
		val bounds	= window.getBounds
		bounds.x		= settings getInt ("mainUI.x", bounds.x)
		bounds.y		= settings getInt ("mainUI.y", bounds.y)
		bounds.width	= settings getInt ("mainUI.w", bounds.width)
		bounds.height	= settings getInt ("mainUI.h", bounds.height)
        UIUtil2 limitAndChangeBounds (window, bounds)
	}
	
	/** stores this UI's state in the properties */
	def saveSettings(settings:Settings) {
		val bounds	= window.getBounds
		settings setInt ("mainUI.x",	bounds.x)
		settings setInt ("mainUI.y",	bounds.y)
		settings setInt ("mainUI.w",	bounds.width)
		settings setInt ("mainUI.h",	bounds.height)
	}
}
