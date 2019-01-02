package commonist.mediawiki

import java.io.InputStream
import java.io.FilterInputStream

import scutil.lang._

final class ProgressInputStream(inputStream:InputStream, callback:Effect[Long]) extends FilterInputStream(inputStream) {
	private var doneBytes	= 0L

	override def read():Int = {
		val tmp		= super.read()
		if (tmp != -1) {
			doneBytes	= doneBytes + 1
			callback(doneBytes)
		}
		tmp
	}

	override def read(b:Array[Byte], off:Int, len:Int):Int = {
		val tmp	= super.read(b, off, len)
		if (tmp > 0)	{
			doneBytes	= doneBytes + tmp
			callback(doneBytes)
		}
		tmp
	}

	override def read(b:Array[Byte]):Int = read(b, 0, b.length)
}
