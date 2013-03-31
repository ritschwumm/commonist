package commonist.ui

import java.io.File
import java.util.{ Stack => JUStack }
import javax.swing._
import javax.swing.event._
import javax.swing.tree._

import scutil.Implicits._
import scutil.Files._
import scutil.SystemProperties
import scutil.gui.CasterInstances._
import scutil.log._

import commonist.Constants
import commonist.util._

/** action events this UI sends */
trait DirectoryUICallback {
	def changeDirectory(currentDirectory:File)
}

/** a JTree used to select a directory in the filesystem */
final class DirectoryUI(callback:DirectoryUICallback) extends JScrollPane with Logging {
	// state
	var currentDirectory	= HOME

	// make a root tree node
	private val (baseNode,fakeRoot)	=
			File.listRoots.guardNotNull.flattenMany.toVector match {
				case Vector(single)	=>
					val baseNode	= new FileNode(single) doto { _.update() }
					(baseNode, false)
				case many	=>
					val baseNode	= new FileNode(UNIX_ROOT)	// fake
					many map (new FileNode(_)) foreach baseNode.add
					(baseNode, true)
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
		val node	= lastNode(ev.getPath)
		node.update()
		directoryModel nodeStructureChanged node
	}
	directoryTree onValueChanged { ev =>
		val node			= lastNode(ev.getPath)
		currentDirectory	= node.file
		callback.changeDirectory(currentDirectory)
	}
	
	private def lastNode(treePath:TreePath):FileNode	=
			treePath.getLastPathComponent.asInstanceOf[FileNode]
	
	//------------------------------------------------------------------------------
	//## BrowseDirectory action

	/** open all directories from the root to a given node and select it */
	def browseDirectory(directory:File) {
		def loop(search:Seq[FileNode], chain:List[File]) {
			search find { _.file == chain.head } match {
				case Some(node)	=>
					val treePath	= node.treePathClone
					directoryTree expandPath treePath
					if (chain.tail.isEmpty) {
						directoryTree	setSelectionPath	treePath
						directoryTree	makeVisible			treePath
						directoryTree	scrollPathToVisible	treePath
						// done
					}
					else {
						loop(node.childNodes, chain.tail)
					}
				case None	=>
					WARN("node not found", chain.head)
			}
		}
		
		val fromRoot	= (directory :: directory.parentChain).reverse
		if (fakeRoot)	loop(baseNode.childNodes,	fromRoot)
		else 			loop(Seq(baseNode),			fromRoot)
	}
	
	//------------------------------------------------------------------------------
	//## Settings

	/** loads this UI's state from the properties */
	def loadSettings(settings:Settings) {
		currentDirectory	= new File(settings getOrElse (
				"directoryTree.currentDirectory", 	
				SystemProperties.user.home))
		browseDirectory(currentDirectory)
	}
	
	/** stores this UI's state in the properties */
	def saveSettings(settings:Settings) {
		settings set ("directoryTree.currentDirectory",	currentDirectory.getPath)
	}
}
