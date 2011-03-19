package commonist.data

/** metadata of a wiki site */
case class WikiData(family:String, site:Option[String],	api:String) {
	// used in the combo box
	override def toString = site match {
		case Some(name)	=> family + ":" + name
		case None		=> family
	}
}
