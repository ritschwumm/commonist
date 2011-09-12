name			:= "commonist"

organization	:= "de.djini"

version			:= "0.4.29"

scalaVersion	:= "2.9.0-1"

//publishArtifact in (Compile, packageBin)	:= false

publishArtifact in (Compile, packageDoc)	:= false

publishArtifact in (Compile, packageSrc)	:= false

libraryDependencies	++= Seq(
	"de.djini"	%% "scutil"		% "0.0.4"	% "compile",
	"de.djini"	%% "scjson"		% "0.0.4"	% "compile",
	"de.djini"	%% "scmw"		% "0.0.3"	% "compile",
	"org.apache.httpcomponents"	% "httpclient"	% "4.1.1"			% "compile",
	"org.apache.httpcomponents"	% "httpmime"	% "4.1.1"			% "compile",
	"org.apache.sanselan"		%  "sanselan"	% "0.97-incubator"	% "compile"
)

scalacOptions	++= Seq("-deprecation", "-unchecked")

//--------------------------------------------------------------------------------

seq(ReflectPlugin.allSettings:_*)

reflectPackage	:= "commonist"

sourceGenerators in Compile <+= reflect map identity

//--------------------------------------------------------------------------------

seq(ScriptStartPlugin.allSettings:_*)

scriptstartMainClass	:= "commonist.Commonist"

scriptstartVmArguments	:= Seq("-Xmx192m")

//--------------------------------------------------------------------------------

seq(WebStartPlugin.allSettings:_*)

webstartMainClass		:= "commonist.Commonist"

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

webstartJnlpConf	:= JnlpConf(
	fileName		= "commonist.jnlp",
	codeBase		= "http://djini.de/software/commonist/ws/",
	title			= "The Commonist",
	vendor			= "FNORD! Inc.",
	description		= "a MediaWiki file upload tool",
	iconName		= Some("commonist-32.png"),
	splashName		= Some("commonist-64.png"),
	offlineAllowed	= true,
	allPermissions	= true,
	j2seVersion		= "1.6+",
	maxHeapSize		= 192
)
