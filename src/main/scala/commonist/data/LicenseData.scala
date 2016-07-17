package commonist.data

/** metadata of a license template */
final case class LicenseData(template:String, description:String) {
	// used in the combo box
	override def toString = template
}
