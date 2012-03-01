package commonist.ui

import java.io.File
import javax.swing.tree.DefaultMutableTreeNode

import scala.collection.mutable

import scutil.Implicits._

/** a TreeNode for a File in the DirectoryTree */
final class FileNode(file:File) extends DefaultMutableTreeNode {
	// getUserObject setUserObject
	
	// state
	
	private var allowsChildrenValue	= false
	
	// API
	
	def childNodes:List[FileNode] = {
		val	out	= new mutable.ListBuffer[FileNode]
		val it	= children()
		while (it.hasMoreElements) {
			val element = it.nextElement.asInstanceOf[FileNode]
			out	+= element
		}
		out.toList
	}
	
	/** get the File this Node stands for */
	def getFile():File = file
	
	/** ensures the node has a single child every directory below it */
	def update() {
		removeAllChildren()
		
		val listed	= file childrenWhere { file:File => file.isDirectory && !file.isHidden }
		allowsChildrenValue	= listed.isDefined
		listed foreach { files =>
			// TODO duplicate
			files sortBy { _.getPath } map { new FileNode(_) } foreach add
		}
	}
	
	// Node
	
	override def getAllowsChildren():Boolean = allowsChildrenValue

	override def isLeaf():Boolean = false
	
	override def toString():String = if (file.getName.length != 0) file.getName else file.getPath
}
