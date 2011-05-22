package commonist

import java.awt.Image
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.MalformedURLException
import java.net.URL

import bsh.Interpreter
import bsh.EvalError

import commonist.data.LicenseData
import commonist.data.WikiData
import commonist.task.ChangeDirectoryTask
import commonist.task.UploadFilesTask
import commonist.thumb.FileCache
import commonist.thumb.Thumbnails
import commonist.ui.CommonUI
import commonist.ui.DirectoryUI
import commonist.ui.DirectoryUICallback
import commonist.ui.ImageListUI
import commonist.ui.MainWindow
import commonist.ui.MainWindowCallback
import commonist.ui.StatusUI
import commonist.ui.UploadUI
import commonist.ui.UploadUICallback
import commonist.util.Loader
import commonist.util.Settings
import commonist.util.Messages

import scutil.Files._
import scutil.Resource._
import scutil.AppleQuit
import scutil.log.Logging
import scutil.gui.SwingUtil._
import scutil.ext.AnyRefImplicits._
import scutil.ext.OptionImplicits._
import scutil.ext.FileImplicits._

/** the main application class */
final class CommonistMain extends Logging {
	private val settingsProp	= (System getProperty "commonist.settings").guardNotNull
	private val settingsDir		= settingsProp map { new File(_) } getOrElse (HOME / ".commonist")
	private val etcDir			= PWD / "etc"
	private val resourcesDir	= PWD / "src" / "main" / "resources"
	private val resourcePrefix	= "/"
	INFO("settings directory: "		+ settingsDir)
	INFO("etc directory: "			+ etcDir)
	INFO("resources directory: "	+ resourcesDir)
	settingsDir.mkdirs()
	require(settingsDir.exists, "settings directory cannot be created")
	private val loader		= new Loader(settingsDir, etcDir, resourcesDir, resourcePrefix)
	
	val programIcon		= null
	val programHeading	= "The Commonist " + Constants.VERSION
	
	private val userLanguage	= System.getProperty("user.language")
	INFO("using user language: " + userLanguage)
	loadMessages(userLanguage)
		
	private val licenses	= loadLicenses()
	private val wikis		= loadWikis()
	require(wikis.nonEmpty, "wiki list may not be empty")
		
	//sourceStartup()
		
	private val settings	= new Settings(settingsDir / "settings.properties")
	private val cache		= new FileCache(settingsDir / "thumbnails.txt", settingsDir / "cache", Constants.THUMBNAIL_CACHE_SIZE)
								
	private val thumbnails	= new Thumbnails(cache)
		
	private val commonUI	= new CommonUI(wikis, licenses)
		
	private val directoryUI	= new DirectoryUI(new DirectoryUICallback {
		def changeDirectory(currentDirectory:File) {
			doChangeDirectory(currentDirectory)
		}
	})
		
	private val statusUI	= new StatusUI()
		
	private val uploadUI	= new UploadUI(new UploadUICallback {
		def startUpload()	{ doStartUpload()	}
		def stopUpload()	{ doStopUpload()	}
	})
		
	private val imageListUI	= new ImageListUI(programHeading, programIcon)
		
	private val mainWindow	= new MainWindow(
			commonUI, directoryUI, imageListUI, statusUI, uploadUI,
			programHeading, programIcon, new MainWindowCallback {
		def quit() { doQuit() }
	})
		
	AppleQuit install doQuit
	
	//-------------------------------------------------------------------------
	//## life cycle
	
	/** startup, called after UI constructors */
	def init() {
		INFO("starting up")
		
		try { cache.load() }
		catch { case e:IOException => ERROR("cannot load cache", e) }
		try { settings.load() }
		catch { case e:IOException => ERROR("cannot load settings", e) }
		
		thumbnails	loadSettings settings
		commonUI	loadSettings settings
		directoryUI	loadSettings settings
		mainWindow	loadSettings settings
		
		mainWindow.makeVisible()
		
		INFO("running")
	}

	/** shutdown */
	def exit() {
		INFO("shutting down")
		
		thumbnails	saveSettings settings
		commonUI	saveSettings settings
		directoryUI	saveSettings settings
		mainWindow	saveSettings settings
		
		try { settings.save() }
		catch { case e:IOException => ERROR("cannot save settings", e) }
		try { cache.save() }
		catch { case e:IOException => ERROR("cannot save cache", e) }
		
		INFO("finished")
	}
	
	//-------------------------------------------------------------------------
	//## actions
	
	/** Action: quit the program */
	private def doQuit() {
		exit()
		System exit 0
	}
	
	private var changeDirectory	= new TaskVar[ChangeDirectoryTask]
	private var uploadFiles		= new TaskVar[UploadFilesTask]
	
	/** 
	 * Action: change to a new directory
	 * load and display imageUIs for all files in the new directory
	 */
	private def doChangeDirectory(directory:File) {
		changeDirectory change new ChangeDirectoryTask(mainWindow, imageListUI, statusUI, thumbnails, directory)
	}
	
	/** Action: start uploading selected files */
	private def doStartUpload() {
		// TODO hack
		if (!imageListUI.getData.hasSelected) {
			INFO("uploading does not make sense when no file is selected")
			return
		}
		uploadFiles change new UploadFilesTask(settingsDir, loader, commonUI.getData, imageListUI.getData, mainWindow, imageListUI, statusUI)
	}
	
	/** Action: stop uploading selected files */
	private def doStopUpload() {
		uploadFiles.abort()
	}
	
	//-------------------------------------------------------------------------
	//## init

	/** load language file for the language or en if not successful and returns the used language */
	private def loadMessages(language:String) {
		val defaultURL	= loader resourceURL "messages_default.properties" getOrError "cannot load messages_default.properties"
		val userLangURL	= loader resourceURL ("messages_" + language + ".properties")
		Messages.init(defaultURL, userLangURL)
	}
	
	/** load licenses */
	private def loadLicenses():List[LicenseData] = 
			Parser parseLicenses (loader resourceURL "licenses.txt" getOrError "cannot load licenses.txt")

	/** load wikis */
	private def loadWikis():List[WikiData] = 
			Parser parseWikis (loader resourceURL "wikis.txt" getOrError "cannot load wikis.txt")
	
	/*
	// loads and executes startup.bsh
	private def sourceStartup() {
		loader resourceURL "startup.bsh" match {
			case Some(url)	=>
				try {
					//interpreter.set("mw", mw)
					new InputStreamReader(url.openStream(), "UTF-8") use { r =>
						val interpreter	= new Interpreter()
						interpreter.eval(r, interpreter.getNameSpace, url.toExternalForm)
					}
				}
				catch { case e:EvalError => ERROR("could not load startup.bsh", e) }
			case None	=>
				INFO("skipping, not found: startup.bsh")
		}
	}
	*/
}
