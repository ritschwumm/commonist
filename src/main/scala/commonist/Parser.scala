package commonist

import java.io._
import java.net._
import java.util.regex._

import scutil.Implicits._
import scutil.log._

import scmw._

import commonist.data._

object Parser extends Logging {
	def parseCategories(s:String):String = {
		val SEPARATOR	= '|'
		val LINK_START	= "[["
		val LINK_END	= "]]"
		
		// if source contains link markers leave it unchanged
		val maybeLink	= (s containsSlice LINK_START) || (s containsSlice LINK_END)
		if (maybeLink)	return s
		
		// else compile wikitext
		s
		.splitAroundChar(SEPARATOR)
		.map { _.trim }
		.filter { _.nonEmpty }
		.map { name => LINK_START + Namespace.category(name) + LINK_END }
		.mkString("\n")
	}
	
	def parseCoordinates(s:String):Option[(String,String)] =
			s splitAroundChar ',' map parseCoordinate match {
				case Seq(Some(latitude), Some(longitude))	=>
					Some((latitude, longitude))
				case _	=> 
					WARN("could not parse coordinates", s)
					None
			}
	def parseCoordinate(s:String):Option[String] =
			s.trim match {
				case ""	=> None
				case x	=> Some(x)
			}
	
	//------------------------------------------------------------------------------

	val WikiDataPattern	= """\s*(\S+)\s+(\S+)\s+(\S+)\s*""".r
	def parseWikis(url:URL):Seq[WikiData] = parseURL(url) {
		_ match {
			case WikiDataPattern(family, site, api)	=> 
				Some(WikiData(family, parseSite(site), api))
			case x => 
				WARN("could not parse line", x)
				None
		}
	}
	def parseSite(s:String):Option[String] = (s != "_") guard s
	
	val	LicenseDataPattern	= """(\{\{[^\}]+\}\})\s*(.*)""".r
	def parseLicenses(url:URL):Seq[LicenseData] = parseURL(url) { 
		_ match {
			case LicenseDataPattern(template, description) => 
				Some(LicenseData(template, description))
			case x => 
				WARN("could not parse line", x)
				None
		}
	}
	
	private def parseURL[T](url:URL)(parseLine:String=>Iterable[T]):Seq[T] = 
			slurpLines(url)
			.map { _.trim } 
			.filter { _.nonEmpty }
			.filter { !_.startsWith("#") } 
			.flatMap { parseLine }
	
	private def slurpLines(url:URL):Seq[String] =
			new BufferedReader(new InputStreamReader(url.openStream, "UTF-8")) use { _.readLines() }
}
