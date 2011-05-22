import sbt._
      
import SbtUtil._

final class CommonistProject(info:ProjectInfo) extends DefaultWebstartProject(info) {
	// dependencies
	val scutil	= "de.djini"	%% "scutil"	% "0.0.3"	% "compile"
	val scjson	= "de.djini"	%% "scjson"	% "0.0.3"	% "compile"
	val scmw	= "de.djini"	%% "scmw"	% "0.0.2"	% "compile"

	// @see http://simple-build-tool.googlecode.com/svn/artifacts/latest/api/sbt/WebstartOptions.html
	
	override def mainClass	= Some("commonist.Commonist")
	
	val memory	= "192m"
	
	// issue compiler warnings
	override def compileOptions	= super.compileOptions ++ Seq(Unchecked)
	
	// @see http://code.google.com/p/simple-build-tool/wiki/ProjectDefinitionExamples
	// lazy val demo = runTask(Some("sample.Main"), testClasspath).dependsOn(testCompile) describedAs "Runs the demo."
	 
	//------------------------------------------------------------------------------
	//## version
	
	val versionPath	= outputPath / "version"
	lazy val versionSource	= task {
		// val gitVersion	= (Process(Seq("git", "rev-parse", "HEAD")) !! log).trim
		FileUtilities write (
				versionPath / "Version.scala" asFile, 
				versionCode("commonist", "Version", version.toString),
				utf_8, 
				log)
		None
	}
	override def mainSourceRoots	= super.mainSourceRoots +++ versionPath.##
	override def compileAction		= super.compileAction dependsOn versionSource
	
	//------------------------------------------------------------------------------
	//## ant helper
	
	lazy val packageHelper = task { None } dependsOn (`package`, packageBinary, packageSource) describedAs "package everything"
	
	//------------------------------------------------------------------------------
	//## src-dist
	
	lazy val packageSource	= task { 
		val srcDistZipPath	= defaultJarPath("-src.zip")
		log.info("zipping src dist to " + srcDistZipPath)
	
		val srcDistExcludes	=
				info.projectPath / ".git"							+++
				info.projectPath / "build.properties"				+++
				info.projectPath / "build.xml"						+++
				info.projectPath / "doc"							+++
				info.projectPath / "etc"							+++
				info.projectPath / "versatz"						+++
				info.projectPath / "project" / "boot"				+++
				info.projectPath / "target"							+++
				info.projectPath / "project" / "build" / "target"
		val srcDistPaths	= ((info.projectPath ##) ** "*") --- (srcDistExcludes ** "*")
		FileUtilities.zip(srcDistPaths.get, srcDistZipPath, false, log)
		None
	} describedAs "Creates an source distribution."

	//------------------------------------------------------------------------------
	//## bin-dist
	
	lazy val packageBinary	= task {
		val binDir		= outputPath / "binary"
		
		log.info("compiling bin dist to " + binDir)
		
		val dependencies		= (dependencyPath ** "*.jar").get
		val managedDependencies	= (managedDependencyPath ** "*.jar").get
		val jarFiles			= List(jarPath, buildLibraryJar) ++ dependencies ++ managedDependencies
		
		val binDistBinPath	= binDir / "bin"
		val binDistLibPath	= binDir / "lib"
		val binDistEtcPath	= binDir / "etc"
	
		FileUtilities copyFlat	(jarFiles,			binDistLibPath,		log)
		FileUtilities sync		(mainResourcesPath,	binDistEtcPath,		log)
		
		// Path.makeString(jarFiles) without leading directories		
		val relativeLibs		= ((binDir ##) / "lib" ** "*.jar").get
		val unixClassPath		= relativeLibs map { _ relativePathString "/" } mkString ":"
		val windowsClassPath	= relativeLibs map { _ relativePathString "\\" } mkString ";"
		val os2ClassPath		= windowsClassPath
		
		val mainClassName	= mainClass getOrElse error("missing main class")
	
		// TODO add a parameter for -Dcommonist.settings=
		val unixScript	= template(
			Map(
				"classPath"		-> unixClassPath,
				"mainClassName"	-> mainClassName,
				"memory"		-> memory
			),
			strip(
				"""
				|	#!/bin/bash
				|	
				|	# change into this script's directory
				|	if which realpath >/dev/null; then
				|		cd "$(dirname "$(realpath "$0")")"
				|	elif which readlink >/dev/null; then
				|		cur="$0"
				|		while [ -n "$cur" ]; do
				|			dir="$(dirname "$cur")"
				|			[ -n "$dir" ] && cd "$dir"
				|			cur="$(readlink "$(basename "$cur")")"
				|		done
				|	elif which perl >/dev/null; then
				|		cd "$(dirname "$(echo "$0" | perl -ne 'use Cwd "abs_path";chomp;print abs_path($_) . "\n"')")"
				|	else
				|		cd "$(dirname "$0")"
				|	fi
				|	
				|	# run the java vm
				|	cd ..
				|	exec java -Xmx{{memory}} -cp {{classPath}} {{mainClassName}} "$@"
				"""
			)
		)
		
		val windowsScript	= template(
			Map(
				"classPath"		-> windowsClassPath,
				"mainClassName"	-> mainClassName,
				"memory"		-> memory
			),
			strip(
				"""
				|	cd /d %~dp0%
				|	cd ..
				|	java -Xmx{{memory}} -cp {{classPath}} {{mainClassName}}
				"""
			)
		)  replace ("\n", "\r\n")
		
		val os2Script	= template(
			Map(
				"classPath"		-> os2ClassPath,
				"mainClassName"	-> mainClassName,
				"memory"		-> memory
			),
			strip(
				"""
				|	cd ..
				|	java -Xmx{{memory}} -cp {{classPath}} {{mainClassName}}
				"""
			)
		) replace ("\n", "\r\n")
		
		val unixScriptFile		= (binDistBinPath / normalizedName).asFile
		val windowsScriptFile	= (binDistBinPath / (normalizedName + ".bat")).asFile
		val os2ScriptFile		= (binDistBinPath / (normalizedName + ".cmd")).asFile
		FileUtilities write	(unixScriptFile,	unixScript,		iso_8859_1, log)
		FileUtilities write	(windowsScriptFile,	windowsScript,	iso_8859_1, log)
		FileUtilities write	(os2ScriptFile,		os2Script,		iso_8859_1, log)
		
		// TODO useless if the zip task doesn't support it
		/*
		import Process._
		<x>chmod 755 {unixScriptFile}</x>	! log
		*/
		
		// (normalizedName + "-" + version.toString + "-bin.zip")
		
		val binDistZipPath	= defaultJarPath("-bin.zip")	// outputPath / (artifactBaseName + "-bin.zip")
		log.info("zipping bin dist to " + binDistZipPath)
		val binDistPaths	= (binDir ##) ** "*"
		FileUtilities zip (binDistPaths.get, binDistZipPath, false, log)
		
		None
	} dependsOn (`package`) describedAs "Creates an binary distribution."
	
	//------------------------------------------------------------------------------
	//## website
	
	/*
	lazy val website	= task {
		val siteDir		= outputPath / "website"
		
		log.info("compiling website to to " + siteDir)
		
		def documents() = {
			def document(name:String):Option[String]	= {
				val slots	= Map(
						"project"	-> projectName.value,
						"version"	-> version.toString)
				FileUtilities readString (mainSourcePath / "doc" / name asFile, utf_8, log) match {
					case Right(text)	=> 
						val	cooked	= template(slots, text)
						FileUtilities write (siteDir / name asFile, cooked, utf_8, log)
					case Left(problem)	=>
						Some(problem)
				}
			}
			document("changes.txt")	orElse
			document("index.html")
		}
		def webstart() = {
			val wsDir		= siteDir / "ws"
			val wsFiles		= (webstartOutputDirectory ##) ** "*" get;
			(FileUtilities copy (wsFiles, wsDir, log)).left.toOption
		}
		
		documents orElse webstart
		
	} dependsOn (`packageBinary`) describedAs "Creates the website distribution."
	*/

	//------------------------------------------------------------------------------
	//## webstart
	
	val keyStoreFile	= (info.projectPath / "etc" / "keyStore").asFile.getAbsoluteFile
	val keyDname		= "CN=Snake Oil, OU=Hacking Unit, O=FNORD! Inc., L=Bielefeld, ST=33641, C=DE"
	val keyPass			= "0xDEADBEEF"
	val storePass		= "0xDEADBEEF"
	val keyAlias		= "signFiles"
	
	lazy val keyGen = task {
		import Process._
		val res	= List("keytool", "-genkey", "-alias", keyAlias, "-dname", keyDname, "-keystore", keyStoreFile.getPath, "-storePass", storePass, "-keypass", keyPass)	! log
		if (res == 0)	None
		else			Some("keytool failed, try deleting " + keyStoreFile)
	} describedAs "Creates a new key for signing webstart jars"
	
	// defaults to artifactBaseName + ".jnlp"
	override def jnlpFileName	= "commonist.jnlp"	
	
	// adds the icons
	override def webstartResources	= webstartExtraResources +++ super.webstartResources
	def webstartExtraResources		= (mainSourcePath / "webstart" ##) ** "*"
	
	def jnlpXML(libraries:Seq[WebstartJarResource]) =
			<jnlp spec="1.0+" codebase="http://djini.de/software/commonist/ws/" href={jnlpFileName}>
				<information>
					<title>The Commonist</title>
					<vendor>FNORD! Inc.</vendor>
					<homepage href="http://djini.de/software/commonist/index.html"/>
					<description>a MediaWiki file upload tool</description>
					<description kind="short">a MediaWiki file upload tool</description>
					<icon href="commonist-32.png"/>
					<icon href="commonist-128.png" kind="splash"/>
					<offline-allowed/>
				</information>
				<security>
					<all-permissions/>
				</security> 
				<resources>
					<j2se version="1.6+" max-heap-size={memory}/>
					{ defaultElements(libraries) }
				</resources>
				<application-desc main-class={mainClass getOrElse error("missing main class")}/>
			</jnlp>
			
	import SignJar._
	
	override def webstartSignConfiguration = Some(new SignConfiguration(
		keyAlias, 
		List( 
			storePassword(storePass),
			keyPassword(keyPass),
			keyStore(keyStoreFile.toURL)
		)
	))
	
	override def webstartPack200	= false
	override def webstartGzip		= false
}
