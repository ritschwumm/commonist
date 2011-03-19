package commonist.util

import scutil.Human

/** text utility functions */
object TextUtil2 {
	/** encode a number of bytes into a human readable form */
	def human(bytes:Long):String = Human.binary(bytes, 1)
	
	/** returns a single-line throwable description */
	def shortError(t:Throwable):String		= t.getClass.getName.replaceAll("^.*\\.", "") + " " + feedToSpace(t.getMessage)
	
	/** replaces every linefeeds with a space */
	def feedToSpace(s:String):String 		= s.replaceAll("\r\n|\r|\n", " ")
	
	/** removes double empty lines */
	def restrictEmptyLines(s:String):String	= s.replaceAll("\n\n\n+", "\n\n")
	
	/** removes linefeeds from both ends of a string */
	def trimLF(s:String):String				= s.replaceAll("^\\n+", "").replaceAll("\\n+$", "")

	/** concatenates two Strings and inserts a separator if both are non-empty */
	def joinNonEmpty(string1:String, string2:String, separator:String):String = 
			List(string1, string2) filter { _.length != 0 } mkString separator
}
