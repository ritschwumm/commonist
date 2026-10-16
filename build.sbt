import xsbtUtil.{ util => xu }

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / versionScheme := Some("early-semver")

name			:= "commonist"
organization	:= "de.djini"
version			:= "1.11.0"

scalaVersion	:= "2.13.7"
scalacOptions	++= Seq(
	"-feature",
	"-deprecation",
	"-unchecked",
	"-Werror",
	"-Xlint",
	"-Xsource:3",
)

conflictManager		:= ConflictManager.strict withOrganization "^(?!(org\\.scala-lang|org\\.scala-js)(\\..*)?)$"
libraryDependencies	++= Seq(
	"de.djini"					%%	"scutil-jdk"	% "0.213.0"			% "compile",
	"de.djini"					%%	"scutil-gui"	% "0.213.0"			% "compile",
	"de.djini"					%%	"scjson-codec"	% "0.239.0"			% "compile",
	"org.apache.sanselan"		%	"sanselan"		% "0.97-incubator"	% "compile",
	"org.apache.httpcomponents"	%	"httpclient"	% "4.5.13"			% "compile",
	"org.apache.httpcomponents"	%	"httpmime"		% "4.5.13"			% "compile"
)

wartremoverErrors ++= Seq(
	Wart.StringPlusAny,
	Wart.EitherProjectionPartial,
	Wart.OptionPartial,
	Wart.Enumeration,
	Wart.FinalCaseClass,
	Wart.JavaConversions,
	Wart.Option2Iterable,
	Wart.TryPartial
)

enablePlugins(ScriptStartPlugin, OsxAppPlugin, KapselPlugin, BuildInfoPlugin)

//--------------------------------------------------------------------------------

buildInfoKeys		:= Seq[BuildInfoKey](name, version)	// name, version, scalaVersion, sbtVersion
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
osxappVm			:= JavaHomeVersion("1.8+")
osxappMainClass		:= Some("commonist.Commonist")
osxappVmOptions		:= Seq("-Xmx192m")

//------------------------------------------------------------------------------

kapselMainClass			:= Some("commonist.Commonist")
kapselVmOptions			:= Seq("-Xmx192m")
kapselMakeExecutable	:= true

//--------------------------------------------------------------------------------

TaskKey[Seq[File]]("bundle")	:= Seq(
	scriptstartZip.value,
	osxappZip.value,
	kapsel.value
)
