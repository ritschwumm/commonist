package commonist.mediawiki

object Namespace {
	def file(name:String):String		= "File:"		+ name
	def user(name:String):String		= "User:"		+ name
	def category(name:String):String	= "Category:"	+ name
	def mediawiki(name:String):String	= "MediaWiki:"	+ name
}
