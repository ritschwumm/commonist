package commonist.ui

import java.io.File
import javax.swing._
import javax.swing.event._
import javax.swing.tree._

import scutil.Files._
import scutil.log.Logging
import scutil.gui.CasterInstances._

import commonist.Constants
import commonist.util._

/** action events this UI sends */
trait DirectoryUICallback {
	def changeDirectory(currentDirectory:File)
}

/** a JTree used to select a directory in the filesystem */
final class DirectoryUI(callback:DirectoryUICallback) extends JScrollPane with Logging {
	// state
	var currentDirectory	= new File(System.getProperty("user.home"))

	// make a root tree node
	private val baseNode	= {
		val rootDirs	= File.listRoots
		val multiRoot	= rootDirs.length > 1
		if (multiRoot) {
			val baseNode1	= new FileNode(new File("/"))	//### FAKE..
			for (rootDir <- rootDirs) {
				baseNode1 add new FileNode(rootDir)
				//### A:\ removed, the tree expand will update later
				// rootNode.update()
			}
			baseNode1
		}
		else {
			val baseNode1	= new FileNode(rootDirs(0))
			baseNode1.update()
			baseNode1
		}
	}
		
	//------------------------------------------------------------------------------
	//## components
	
	private val directoryModel	= new DefaultTreeModel(baseNode)
	private val directoryTree	= new JTree
	directoryTree setModel directoryModel
	//directoryTree setRootVisible false
	directoryTree.getSelectionModel setSelectionMode TreeSelectionModel.SINGLE_TREE_SELECTION
	
	setViewportView(directoryTree)
	setBorder(Constants.PANEL_BORDER)
	
	//------------------------------------------------------------------------------
	//##  wiring
	
	directoryTree onTreeExpanded { ev =>
		val node	= ev.getPath.getLastPathComponent.asInstanceOf[FileNode]
		node.update()
		directoryModel nodeStructureChanged node
	}
	directoryTree onValueChanged { ev =>
		val node	= ev.getPath.getLastPathComponent.asInstanceOf[FileNode]
		currentDirectory	= node.getFile
		callback.changeDirectory(currentDirectory)
	}
	
	//------------------------------------------------------------------------------
	//## BrowseDirectory action

	/** browses all directories from the root to a given directory */
	def browseDirectory(directory1:File) {
		var directory:File	= directory1
		
		// build stack
		val stack	= new java.util.Stack[File]	// TODO use scala collection
		while (directory != null) {
			stack push directory
			directory	= directory.getParentFile
		}
		
		// find root node
		var node:FileNode	= null	// TODO make immutable
		directory	= stack.pop()
		node	=
				if (baseNode.getFile == directory)	baseNode
				else 								baseNode.childNodes find { _.getFile == directory } orNull;
		// TODO use Option	
		if (node == null)	{ WARN("first node not found!"); return }
	
		val path1	= cloneTreePath(node)
		directoryTree expandPath path1
		
		//------------------------------------------------------------------------------
		
		while (!stack.empty()) {
			directory	= stack.pop()
			// TODO use Option
			node	= node.childNodes find { _.getFile == directory } orNull;
			if (node == null)	{ WARN("child node not found!"); return; }
			
			val path2	= cloneTreePath(node)
			directoryTree expandPath path2
		}
	
		// does not get visible.. why?
		val path3	= cloneTreePath(node)
		directoryTree expandPath		path3
		directoryTree makeVisible		path3
		directoryTree setSelectionPath	path3
	}
	
	// NOTE without asInstanceOf scala chooses the Object constructor over the Object[] constructor
	private def cloneTreePath(node:DefaultMutableTreeNode):TreePath = 
			new TreePath(node.getPath.asInstanceOf[Array[Object]])
	
	//------------------------------------------------------------------------------
	//## Settings

	/** loads this UI's state from the properties */
	def loadSettings(settings:Settings) {
		currentDirectory	= new File(settings get ("directoryTree.currentDirectory", 	System.getProperty("user.home")))
		browseDirectory(currentDirectory)
	}
	
	/** stores this UI's state in the properties */
	def saveSettings(settings:Settings) {
		settings set ("directoryTree.currentDirectory",	currentDirectory.getPath)
	}
}
