import scala.xml._
import xsbtWebStart.Import.JnlpAsset

/** workaround for https://github.com/sbt/sbt/issues/1738 */
object Jnlp {
	def descriptor(fileName:String, assets:Seq[JnlpAsset]):Elem	=
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
