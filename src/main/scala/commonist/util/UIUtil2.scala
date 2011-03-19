package commonist.util

import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusAdapter

import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.JComponent
import javax.swing.KeyStroke


/** swing UI utility functions */
object UIUtil2 {
	def scrollVisibleOnFocus(focusSource:JComponent, visibleTarget:JComponent) {
		focusSource addFocusListener new FocusAdapter {
			override def focusGained(ev:FocusEvent) {
				val bounds	= new Rectangle(0, 0, visibleTarget.getWidth, visibleTarget.getHeight)
				visibleTarget scrollRectToVisible bounds
				visibleTarget.repaint()
			}
		}
	}
	
	private val TAB_PLAIN	= KeyStroke getKeyStroke (KeyEvent.VK_TAB, 0)
	private val TAB_SHIFT	= KeyStroke getKeyStroke (KeyEvent.VK_TAB, InputEvent.SHIFT_MASK)
	def tabMovesFocus(target:JComponent) {
		target setFocusTraversalKeys (KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,  java.util.Collections.singleton(TAB_PLAIN))
		target setFocusTraversalKeys (KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, java.util.Collections.singleton(TAB_SHIFT))
						
		/*
		// @see http://www.javalobby.org/java/forums/t20457.html
		
		import java.awt.Component
		import java.awt.KeyboardFocusManager
		import java.awt.event.ActionListener
		import java.awt.event.ActionEvent
		import javax.swing.Action
		import javax.swing.AbstractAction
		import javax.swing.JComponent

		val nextFocusAction:Action	= new AbstractAction("Move Focus Forwards") { 
			def actionPerformed(ev:ActionEvent) { 
				ev.getSource.asInstanceOf[Component].transferFocus()
			}
		}
		val prevFocusAction:Action	= new AbstractAction("Move Focus Backwards") {
			def actionPerformed(ev:ActionEvent) {
				ev.getSource.asInstanceOf[Component].transferFocusBackward()
			}
		}
		
		target.getActionMap put (nextFocusAction getValue Action.NAME, nextFocusAction)
		target.getActionMap put (prevFocusAction getValue Action.NAME, prevFocusAction)
		*/
	}
	
	/** sets window bounds limited to the screen estate */ 
	def limitAndChangeBounds(window:Window, bounds:Rectangle) {
		val screen = screenRect(
				Toolkit.getDefaultToolkit,
				window.getGraphicsConfiguration)
		val limited	= boundsWithinScreen(bounds, screen)
		window.setBounds(limited)
	}
	
	/** limit a Rectangle to the screen boundaries */
	def boundsWithinScreen(window:Rectangle, screen:Rectangle):Rectangle = {
		val out	= new Rectangle(window)
		if (out.width  > screen.width)			out.width	= screen.width
		if (out.height > screen.height)			out.height	= screen.height
		if (out.x < screen.x)					out.x	= screen.x
		if (out.y < screen.y)					out.y	= screen.y
		if (out.x + out.width  > screen.width)	out.x	= screen.width  - out.width
		if (out.y + out.height > screen.height)	out.y	= screen.height - out.height
		out
	}

	/** gets the screen estate */
	def screenRect(toolkit:Toolkit, gc:GraphicsConfiguration):Rectangle = {
		val bounds	= gc.getBounds
		val insets	= toolkit.getScreenInsets(gc)
		bounds.x		+= insets.left
		bounds.y		+= insets.top
		bounds.width	-= insets.left + insets.right
		bounds.height	-= insets.top  + insets.bottom
		bounds
	}
	
	/** scrolls a JScrollPane such that the center of the content is the center of the viewPort */
	def scrollToCenter(scroll:JScrollPane) {
		val vp			= scroll.getViewport
		val viewSize	= vp.getViewSize
		val extentSize	= vp.getExtentSize
		val left	= (viewSize.width  - extentSize.width)  / 2
		val top		= (viewSize.height - extentSize.height) / 2
		val pos		= vp.getViewPosition
		if (left >= 0) pos.x	= left	
		if (top  >= 0) pos.y	= top
		vp.setViewPosition(pos)
	}
	
	/** limits a Point to the insides of a Rectangle */
	def limitToBounds(point:Point, bounds:Dimension):Point = new Point(
			limitToBounds(point.x, 0, bounds.width),
			limitToBounds(point.y, 0, bounds.height))
	
	/** limits an int value to given lower and upper bounds */
	private def limitToBounds(value:Int, minInclusive:Int, maxExclusive:Int):Int =
				 if (value < minInclusive)	minInclusive
			else if (value >= maxExclusive)	maxExclusive-1
			else							value
}
