package commonist.mediawiki

import java.util.{ArrayList=>JList}
import java.io.File
import java.net.ProxySelector

import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpException
import org.apache.http.NameValuePair
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicNameValuePair
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.protocol.HttpContext
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.util.EntityUtils

import scutil.base.implicits._
import scutil.lang._
import scutil.log._

import scjson.ast._
import scjson.codec._

import commonist.BuildInfo
import commonist.mediawiki.web._

final class Connection(apiURL:String) extends Logging {
	//private val apiTarget	= (URIData parse apiURL).target getOrError ("invalid api url: " + apiURL)
	private val charSet		= Charsets.utf_8
	private val userAgent	= BuildInfo.name + "/" + BuildInfo.version
	
	@volatile
	private var cred:Option[Cred]	= None
	
	private val manager	= new PoolingHttpClientConnectionManager
	manager setDefaultMaxPerRoute	6
	manager setMaxTotal				18
	
	private val routePlanner	= new SystemDefaultRoutePlanner(
			ProxySelector.getDefault)
			
	// NOTE new for 4.3.1
	private object PreemptiveInterceptor extends HttpRequestInterceptor  {
		@throws(classOf[HttpException])
		def process(request:HttpRequest, context:HttpContext) {
			cred foreach { cred =>
				val credentials	= new UsernamePasswordCredentials(cred.user, cred.password)
				val scheme		= new BasicScheme(charSet)
				val header		= scheme authenticate (credentials, request, context)
				request addHeader header
			}
		}
	}
	
	private val client	=
			HttpClientBuilder
			.create()
			.setConnectionManager(manager)
			.setRoutePlanner(routePlanner)
			.addInterceptorLast(PreemptiveInterceptor)
			.disableRedirectHandling()
			.setDefaultHeaders(arrayList(ISeq(new BasicHeader("User-Agent", userAgent))))
			.build()
	// TODO check we can do without this
	// client.getParams setParameter (ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
	
	def dispose() {
		manager.shutdown()
	}
	
	//------------------------------------------------------------------------------
	
	/**  set credentials to be used for authentication */
	def identify(cred:Option[Cred]) {
		this.cred	= cred
	}
	
	def GET(params:ISeq[(String,String)]):Option[JsonValue] = {
		val	queryString	= URLEncodedUtils format (nameValueList(params), charSet.name)
		val	request		= new HttpGet(apiURL + "?" + queryString)
		handle(request)
	}
	
	def POST(params:ISeq[(String,String)]):Option[JsonValue] = {
		val	requestEntity	= new UrlEncodedFormEntity(nameValueList(params), charSet.name)
		val request			= new HttpPost(apiURL)
		request	setEntity	requestEntity
		handle(request)
	}
	
	def POST_multipart(params:ISeq[(String,String)], fileField:String, file:File, progress:Long=>Unit):Option[JsonValue] = {
		// val requestEntity	= new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, null)
		
		val builder	= MultipartEntityBuilder.create()
		params foreach { case (key, value)	=>
			// NOTE was Content-Transfer-Encoding: 8bit
			builder addTextBody (key, value, ContentType.TEXT_PLAIN withCharset charSet)
		}
		// NOTE was Content-Transfer-Encoding: binary
		builder addPart(fileField, new ProgressFileBody(file, ContentType.APPLICATION_OCTET_STREAM, progress))
		val requestEntity	= builder.build()
		
		val request			= new HttpPost(apiURL)
		request	setEntity	requestEntity
		handle(request)
	}
	
	private def handle(request:HttpUriRequest):Option[JsonValue] = {
		DEBUG(request.getRequestLine.toString)
		val	response		= client execute request
		try {
			DEBUG(response.getStatusLine.toString)
			require(
					response.getStatusLine.getStatusCode == 200,	
					"unexpected response: " + response.getStatusLine.toString)
			response.getEntity.optionNotNull map EntityUtils.toString flatMap { it => (JsonCodec decode it).toOption }
		}
		finally {
			response.close()
		}
	}
	
	private def nameValueList(kv:ISeq[(String,String)]):JList[NameValuePair]	=
			arrayList(kv map nameValue)
			
	private def nameValue(kv:(String,String)):NameValuePair	=
			new BasicNameValuePair(kv._1, kv._2)
	
	private def arrayList[T](elements:ISeq[T]):JList[T]	=
			new JList[T] |>> { al => elements foreach al.add }
}
