package commonist.ui

import java.io._
import java.awt.{ List => AwtList, _ }
import java.awt.event._
import java.awt.image._
import javax.swing._
import javax.imageio._

import scutil.gui.SwingUtil._
import scutil.log._

import commonist.util._

object FullImageWindow extends Logging {
	def display(file:File, programHeading:String, programIcon:Image) {
		worker {
			try {
				val image = ImageIO read file
				if (image != null) {
					edt {
						new FullImageWindow(file, programHeading, programIcon, image)
					}
				}
				else {
					WARN("cannot load image: " + file)
				}
			}
			catch { case e:IOException =>
				WARN("cannot load image: " + file, e)
			}
		}
	}
}

/** displays a single image in full size */
final class FullImageWindow(file:File, programHeading:String, programIcon:Image, image:Image) {
	val icon	= new ImageIcon(image)
	val label	= new ScrollablePicture
	label setHorizontalAlignment	SwingConstants.CENTER
	label setVerticalAlignment		SwingConstants.CENTER
	label setIcon icon
	
	val scroll	= new JScrollPane(label)
	
	val heading	= file.getName + " - " + programHeading
	val window	= new JFrame(heading)
	window setIconImage programIcon
	window.getContentPane add scroll
	window.pack()
	
//		// TODO: seems to break with small images
//		Rectangle	bounds	= UIUtil.boundsInScreen(window.getBounds)
//		bounds.width	= Math.max(bounds.width,	Constants.FULLSIZE_MIN_FRAME_SIZE)
//		bounds.height	= Math.max(bounds.height,	Constants.FULLSIZE_MIN_FRAME_SIZE)
//		window.setBounds(bounds)
	//window.MaximumSize				= window.Size
	//statt dessen evtl. MaximizedBounds einsetzen
	
	UIUtil2 limitAndChangeBounds (window, window.getBounds)
	
	window setDefaultCloseOperation WindowConstants.DISPOSE_ON_CLOSE
	window setLocationRelativeTo null
	UIUtil2 scrollToCenter scroll
	window setVisible true
	
	window.getRootPane.registerKeyboardAction(
		new ActionListener { 
			def actionPerformed(ev:ActionEvent) {
				window.dispose()
			}
		},
		KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
		JComponent.WHEN_IN_FOCUSED_WINDOW
	)
}
