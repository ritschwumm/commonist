package commonist.mediawiki

import java.io._

import scutil.lang._

import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.content.AbstractContentBody
import org.apache.http.entity.mime.MIME

import scutil.core.implicits._
import scutil.jdk.implicits._

final class ProgressFileBody(file:File, contentType:ContentType, progress:Effect[Long]) extends AbstractContentBody(contentType) {
	require(file != null, "File may not be null")

	def getInputStream():InputStream	=
		new ProgressInputStream(file.newInputStream(), progress)

	override def writeTo(out:OutputStream):Unit	= {
		require(out != null, "Output stream may not be null")
		getInputStream() use { _ transferToPre9 out }
		out.flush()
	}

	def getTransferEncoding():String	= MIME.ENC_BINARY
	def getContentLength():Long			= file.length
	def getFilename():String			= file.getName
	def getFile():File					= file
}
