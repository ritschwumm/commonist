name			:= "commonist"

organization	:= "de.djini"

version			:= "0.9.0"

scalaVersion	:= "2.10.3"

libraryDependencies	++= Seq(
	"de.djini"					%%	"scutil"		% "0.40.0"			% "compile",
	"de.djini"					%%	"scmw"			% "0.40.0"			% "compile",
	"org.apache.sanselan"		%	"sanselan"		% "0.97-incubator"	% "compile",
	"org.simplericity.macify"	%	"macify"		% "1.6"				% "compile"
)

scalacOptions	++= Seq(
	"-deprecation",
	"-unchecked",
	"-language:implicitConversions",
	// "-language:existentials",
	// "-language:higherKinds",
	// "-language:reflectiveCalls",
	// "-language:dynamics",
	"-language:postfixOps",
	// "-language:experimental.macros"
	"-feature"
)

//--------------------------------------------------------------------------------

buildInfoSettings

sourceGenerators in Compile	<+= buildInfo

buildInfoKeys		:= Seq[BuildInfoKey](version)	// name, version, scalaVersion, sbtVersion

buildInfoPackage	:= "commonist"

//--------------------------------------------------------------------------------

scriptstartSettings

scriptstartConfigs	:= Seq(ScriptConfig(
	scriptName	= "commonist",
	vmArguments	= Seq("-Xmx192m"),
	mainClass	= "commonist.Commonist"
))

// scriptstart::zipper
inTask(scriptstartBuild)(zipperSettings ++ Seq(
	zipperFiles	:= selectSubpaths(scriptstartBuild.value, -DirectoryFilter).toSeq
))

//--------------------------------------------------------------------------------

osxappSettings

osxappBundleName	:= "commonist"

osxappBundleIcons	:= baseDirectory.value / "src/main/osxapp/commonist.icns"

osxappVm			:= OracleJava7()

osxappMainClass		:= Some("commonist.Commonist")

osxappVmOptions		:= Seq("-Xmx192m")

// osxapp::zipper
inTask(osxappBuild)(zipperSettings ++ Seq(
	zipperFiles		:= selectSubpaths(osxappBuild.value, -DirectoryFilter).toSeq,
	zipperBundle	:= zipperBundle.value + ".app" 
))

//--------------------------------------------------------------------------------

webstartSettings

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
	descriptor	= (fileName:String, assets:Seq[JnlpAsset]) => {
		<jnlp spec="1.6+" codebase="http://djini.de/software/commonist/ws/" href={fileName}>
			<information>
				<title>The Commonist</title>
				<vendor>FNORD! Inc.</vendor>
				<description>a MediaWiki file upload tool</description>
				<icon href="commonist-32.png"/>
				<icon href="commonist-64.png" kind="splash"/>
				<offline-allowed/>
			</information>
			<security>
				<all-permissions/>
			</security> 
			<resources>
				<j2se version="1.6+" max-heap-size="192m"/>
				{ assets map { _.toElem } }
			</resources>
			<application-desc main-class="commonist.Commonist"/>
		</jnlp>
	}
))

webstartExtras	:= Path selectSubpaths ((sourceDirectory in Compile).value / "webstart" , -DirectoryFilter) toSeq
