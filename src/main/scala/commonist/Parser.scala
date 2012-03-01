package commonist

import java.io._
import java.net._
import java.util.regex._

import scutil.Implicits._
import scutil.Resource._
import scutil.log.Logging

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
		.splitAround(SEPARATOR)
		.map { _.trim }
		.filter { _.nonEmpty }
		.map { name => LINK_START + Namespace.category(name) + LINK_END }
		.mkString("\n")
	}
	
	def parseCoordinates(s:String):Option[Pair[String,String]] =
			s splitAround ',' map { parseCoordinate _ } match {
				case Seq(Some(latitude), Some(longitude))	=>
					Some(Pair(latitude, longitude))
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
	def parseWikis(url:URL):List[WikiData] = parseURL(url) {
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
	def parseLicenses(url:URL):List[LicenseData] = parseURL(url) { 
		_ match {
			case LicenseDataPattern(template, description) => 
				Some(LicenseData(template, description))
			case x => 
				WARN("could not parse line", x)
				None
		}
	}
	
	private def parseURL[T](url:URL)(parseLine:String=>Iterable[T]):List[T] = 
			slurpLines(url)
			.map { _.trim } 
			.filter { _.nonEmpty }
			.filter { !_.startsWith("#") } 
			.flatMap { parseLine }
	
	private def slurpLines(url:URL):List[String] = {
		import java.io._
		import scala.collection.mutable.ListBuffer
		import scala.annotation.tailrec
		
		new BufferedReader(new InputStreamReader(url.openStream, "UTF-8")) use { in =>
			val out	= new ListBuffer[String]
			@tailrec def readLine() {
				val	line	= in.readLine
				if (line != null) {
					out	+= line
					readLine()
				}
			}
			readLine()
			out.toList
		}
	}
}
