import sbt._
    
import java.nio.charset.Charset
import scala.util.matching.Regex

object SbtUtil {
	//------------------------------------------------------------------------------
	//## charsets
	
	val utf_8		= Charset forName "UTF-8"
	val iso_8859_1	= Charset forName "ISO-8859-1"
	
	//------------------------------------------------------------------------------
	//## text manipulation
	
	val Strip	= new Regex("""^\s*\|\t(.*)$""")
	def strip(s:String):String	= 
			s.lines.toList flatMap {
				case Strip(it:String)	=> Some(it)
				case _					=> None 
			} mkString "\n"
	
	def template(args:Iterable[Pair[String,String]], s:String):String	= 
			args.toList.foldLeft(s) { case (s,(k,v)) =>
				s replace ("{{"+k+"}}", v)
			}
			
	/** expects unix line endings */
	def expandTabs(width:Int, text:String):String = {
		val	out	= new StringBuilder
		var col	= 0
		var i	= 0
		while (i < text.length) {
			text charAt i match {
				case '\n'	=> 
					out append '\n'
					col	= 0
				case '\t'	=>
					do {
						out append ' '
						col	+= 1
					}
					while (col % width != 0)
				case x	=>
					out append x
					col	+= 1
			} 
			i	+= 1
		}
		out.toString
	}
		
	//------------------------------------------------------------------------------
	//## version reflection
	
	def versionCode(packageName:String, objectName:String, versionName:String):String	=
			template(
				Map(
					"packageName"	-> packageName,
					"objectName"	-> objectName,
					"versionName"	-> versionName
				),
				strip(
					"""
					|	package {{packageName}}
					|	object {{objectName}} {
					|		def project:String = "{{versionName}}"
					|	}
					"""
				)
			)
}
