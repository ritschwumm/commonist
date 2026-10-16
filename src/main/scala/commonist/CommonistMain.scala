package commonist

import java.io._
import java.awt._
import javax.imageio.ImageIO

import scutil.core.implicits._
import scutil.jdk.implicits._
import scutil.platform._
import scutil.log._

import commonist.data._
import commonist.task._
import commonist.thumb._
import commonist.ui._
import commonist.util._

/** the main application class */
object CommonistMain extends Logging {
	def onStartupError(e:Exception):Unit	= {
		e.printStackTrace()
	}
	ExceptionUtil logAllExceptions { (thread,throwable) =>
		throwable.printStackTrace()
	}
	ExceptionUtil logAWTExceptions { (thread,throwable) =>
		throwable.printStackTrace()
	}

	private val settingsProp	= (System getProperty "commonist.settings").optionNotNull
	private val settingsDir		= settingsProp map { new File(_) } getOrElse (Platform.homeDir / ".commonist")
	private val etcDir			= Platform.currentDir / "etc"
	private val resourcesDir	= Platform.currentDir / "src" / "main" / "resources"
	private val resourcePrefix	= "/"
	INFO("settings directory",	settingsDir)
	INFO("etc directory",		etcDir)
	INFO("resources directory",	resourcesDir)
	settingsDir.mkdirs()
	require(settingsDir.exists, "settings directory cannot be created")
	private val loader		= new Loader(settingsDir, etcDir, resourcesDir, resourcePrefix)

	val programIcon		= null
	val programHeading	= show"The Commonist ${commonist.BuildInfo.version}"

	private val userLanguage	= SystemProperties.user.language
	INFO("using user language", userLanguage)
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
		def changeDirectory(currentDirectory:File):Unit	= {
			doChangeDirectory(currentDirectory)
		}
	})

	private val statusUI	= new StatusUI()

	private val uploadUI	= new UploadUI(new UploadUICallback {
		def startUpload():Unit	=	{ doStartUpload()	}
		def stopUpload():Unit	=	{ doStopUpload()	}
	})

	private val imageListUI	= new ImageListUI(programHeading, programIcon)

	private val mainWindow	=
		new MainWindow(
			commonUI, directoryUI, imageListUI, statusUI, uploadUI,
			programHeading, programIcon,
			new MainWindowCallback {
				def quit():Unit	= { doQuit() }
			}
		)

	// TODO use setAboutHandler to display out own dialog

	try {
		Desktop.getDesktop setQuitHandler { (ev, res) =>
			doQuit()
		}
	}
	catch { case e:UnsupportedOperationException =>
		//e.printStackTrace()
	}

	try {
		Taskbar.getTaskbar setIconImage ("/commonist-128.png" |> getClass.getResource |> ImageIO.read)
	}
	catch { case e:UnsupportedOperationException =>
		//e.printStackTrace()
	}

	//-------------------------------------------------------------------------
	//## life cycle

	/** startup, called after UI constructors */
	def init():Unit	= {
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
	def exit():Unit	= {
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
	private def doQuit():Unit	= {
		exit()
		System exit 0
	}

	private val changeDirectory	= new TaskVar[ChangeDirectoryTask]
	private val uploadFiles		= new TaskVar[UploadFilesTask]

	/**
	 * Action: change to a new directory
	 * load and display imageUIs for all files in the new directory
	 */
	private def doChangeDirectory(directory:File):Unit	= {
		changeDirectory change new ChangeDirectoryTask(mainWindow, imageListUI, statusUI, thumbnails, directory)
	}

	/** Action: start uploading selected files */
	private def doStartUpload():Unit	= {
		// TODO hack
		if (!imageListUI.getData.hasSelected) {
			INFO("uploading does not make sense when no file is selected")
			return
		}
		uploadFiles change new UploadFilesTask(settingsDir, loader, commonUI.getData(), imageListUI.getData, mainWindow, imageListUI, statusUI)
	}

	/** Action: stop uploading selected files */
	private def doStopUpload():Unit	= {
		uploadFiles.abort()
	}

	//-------------------------------------------------------------------------
	//## init

	/** load language file for the language or en if not successful and returns the used language */
	private def loadMessages(language:String):Unit	= {
		val defaultURL	= loader resourceURL "messages_default.properties" getOrError "cannot load messages_default.properties"
		val userLangURL	= loader resourceURL (show"messages_${language}.properties")
		Messages.init(defaultURL, userLangURL)
	}

	/** load licenses */
	private def loadLicenses():Seq[LicenseData] =
		Parser parseLicenses (loader resourceURL "licenses.txt" getOrError "cannot load licenses.txt")

	/** load wikis */
	private def loadWikis():Seq[WikiData] =
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

	//------------------------------------------------------------------------------
	//## startup

	init()
}
