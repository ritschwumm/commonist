name			:= "commonist"

organization	:= "de.djini"

version			:= "0.4.32"

scalaVersion	:= "2.9.2"

libraryDependencies	++= Seq(
	"de.djini"					%%	"scutil"		% "0.14.0"			% "compile",
	"de.djini"					%%	"scjson"		% "0.14.0"			% "compile",
	"de.djini"					%%	"scmw"			% "0.9.0"			% "compile",
	"org.apache.httpcomponents"	%	"httpclient"	% "4.2.2"			% "compile",
	"org.apache.httpcomponents"	%	"httpmime"		% "4.2.2"			% "compile",
	"org.apache.sanselan"		%	"sanselan"		% "0.97-incubator"	% "compile"
)

scalacOptions	++= Seq("-deprecation", "-unchecked")

//--------------------------------------------------------------------------------

buildInfoSettings

sourceGenerators in Compile	<+= buildInfo

buildInfoKeys		:= Seq[Scoped](version)	// name, version, scalaVersion, sbtVersion

buildInfoPackage	:= "commonist"

//--------------------------------------------------------------------------------

scriptstartSettings

scriptstartScriptConf	:= Seq(ScriptConf(
	scriptName	= "commonist",
	vmArguments	= Seq("-Xmx192m"),
	mainClass	= "commonist.Commonist"
))

//--------------------------------------------------------------------------------

osxappSettings

osxappBundleName	:= "commonist"

osxappBundleIcons	:= file("src/main/osxapp/commonist.icns")

osxappJvmVersion	:= "1.6+"

osxappMainClass		:= Some("commonist.Commonist")

osxappVmOptions		:= Seq(
	"-Xmx192m"
)

//--------------------------------------------------------------------------------

webstartSettings

webstartGenConf	:= GenConf(
	dname		= "CN=Snake Oil, OU=Hacking Unit, O=FNORD! Inc., L=Bielefeld, ST=33641, C=DE",
	validity	= 365
)

webstartKeyConf	:= KeyConf(
	keyStore	= file("etc/keyStore"),
	storePass	= "0xDEADBEEF",
	alias		= "signFiles",
	keyPass		= "0xDEADBEEF"
)

webstartJnlpConf	:= Seq(JnlpConf(
	fileName	= "commonist.jnlp",
	descriptor	= (fileName:String, assets:Seq[JnlpAsset]) => {
		<jnlp spec="1.5+" codebase="http://djini.de/software/commonist/ws/" href={fileName}>
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
