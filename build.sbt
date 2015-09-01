name			:= "commonist"
organization	:= "de.djini"
version			:= "1.3.0"

scalaVersion	:= "2.11.7"
scalacOptions	++= Seq(
	"-deprecation",
	"-unchecked",
	"-language:implicitConversions",
	// "-language:existentials",
	// "-language:higherKinds",
	// "-language:reflectiveCalls",
	// "-language:dynamics",
	// "-language:postfixOps",
	// "-language:experimental.macros"
	"-feature",
	"-Ywarn-unused-import",
	"-Xfatal-warnings"
)

conflictManager	:= ConflictManager.strict
libraryDependencies	++= Seq(
	"de.djini"					%%	"scutil-core"	% "0.71.0"			% "compile",
	"de.djini"					%%	"scutil-swing"	% "0.71.0"			% "compile",
	"de.djini"					%%	"scmw"			% "0.72.0"			% "compile",
	"org.apache.sanselan"		%	"sanselan"		% "0.97-incubator"	% "compile",
	"org.simplericity.macify"	%	"macify"		% "1.6"				% "compile"
)

enablePlugins(WebStartPlugin, ScriptStartPlugin, OsxAppPlugin, CapsulePlugin)

//--------------------------------------------------------------------------------

buildInfoSettings
sourceGenerators in Compile	<+= buildInfo
buildInfoKeys		:= Seq[BuildInfoKey](version)	// name, version, scalaVersion, sbtVersion
buildInfoPackage	:= "commonist"

//--------------------------------------------------------------------------------

scriptstartConfigs	:= Seq(ScriptConfig(
	scriptName	= "commonist",
	vmOptions	= Seq("-Xmx192m"),
	mainClass	= "commonist.Commonist"
))

//--------------------------------------------------------------------------------

osxappBundleName	:= "commonist"
osxappBundleIcons	:= baseDirectory.value / "src/main/osxapp/commonist.icns"
osxappVm			:= OracleJava7()
osxappMainClass		:= Some("commonist.Commonist")
osxappVmOptions		:= Seq("-Xmx192m")

//------------------------------------------------------------------------------

capsuleMainClass		:= Some("commonist.Commonist")
capsuleVmOptions		:= Seq("-Xmx192m")
capsuleMinJavaVersion	:= Some("1.7.0")
capsuleMakeExecutable	:= true

//--------------------------------------------------------------------------------

webstartGenConfig	:= Some(GenConfig(
	dname		= "CN=Snake Oil, OU=Hacking Unit, O=FNORD! Inc., L=Bielefeld, ST=33641, C=DE",
	validity	= 365
))
webstartKeyConfig	:= Some(KeyConfig(
	keyStore	= baseDirectory.value / "etc/keyStore",
	storePass	= "0xDEADBEEF",
	alias		= "signFiles",
	keyPass		= "0xDEADBEEF"
))
webstartManifest	:= Some(baseDirectory.value / "etc/manifest.mf")
webstartJnlpConfigs	:= Seq(JnlpConfig(
	fileName	= "commonist.jnlp",
	descriptor	= Jnlp.descriptor
))
webstartExtras	:= selectSubpaths((sourceDirectory in Compile).value / "webstart" , -DirectoryFilter) toSeq

//------------------------------------------------------------------------------

TaskKey[Seq[File]]("bundle")	:= Seq(
	scriptstartZip.value,
	osxappZip.value,
	capsule.value
)
