package commonist.thumb

import java.io._

import scutil.Implicits._
import scutil.Resource._
import scutil.log.Logging

/** caches drived Files indexed by their original Files */
final class FileCache(list:File, directory:File, cachedFiles:Int) extends Logging {
	directory.mkdirs()
	
	// TODO use scala collections
	val entryList	= new java.util.LinkedList[File]
	val entryMap	= new java.util.HashMap[File,File]

	/** get a cachefile if it exists */
	def get(original:File):Option[File] = {
		val cached	= entryMap get original
		if (cached == null)	return None
		
		if (!cached.exists || original.lastModified > cached.lastModified) {
			remove(original)
			return None
		}
		
		// move to the end of the list (LRU)
		entryList remove	original
		entryList add		original
		Some(cached)
	}
	
	/** create a new cachefile */
	def put(original:File):File = {
		flush()
		
		// insert a new entry
		val	cached	= cacheFile()
		INFO("caching original: " + original)
		INFO("cached thumbnail: " + cached)
		entryList	add	original
		entryMap	put	(original, cached)
		cached
	}

	/** remove an entry */
	def remove(original:File) {
		val cached	= entryMap get original
		DEBUG("removing original: " + original)
		entryList	remove original
		entryMap	remove original
		if (cached != null && cached.exists) {
			INFO("deleting cached: " +  cached)
			cached.delete()
		}
	}
	
	/** load cache metadata */
	def load() {
		INFO("loading")
		clear()
		if (!list.exists)	return
		
		DEBUG("reading metadata", list)
		new BufferedReader(new InputStreamReader(new FileInputStream(list), "UTF-8")) use { in =>
			try {
				var	running	= true
				while (running) {
					val separator	= in.readLine
					if (separator != null) {
						val original	= new File(in.readLine)
						val cached		= new File(in.readLine)
						entryList	add	original
						entryMap	put	(original, cached)
					}
					else {
						running = false
					}
				}
			}
			catch {
				case e:Exception	=> WARN("cannot load metadata cache", list, e)
			}
		}
	}
	
	/** store cache metadata */
	def save() {
		INFO("saving")
		cleanup()

		DEBUG("writing metadata", list)
		new OutputStreamWriter(new FileOutputStream(list), "UTF-8") use { out =>
			val it	= entryList.iterator
			while (it.hasNext) {
				val original	= it.next()
				val cached		= entryMap get original
				out write "\n"
				out write original.getPath
				out write "\n"
				out write cached.getPath
				out write "\n"
			}
		}
	}
	
	/** clear cache metadata */
	private def clear() {
		entryList.clear()
		entryMap.clear()
	}
	
	/** remove the oldest cache entry and delete its file */
	private def flush() {
		if (entryList.isEmpty() || entryList.size() <= cachedFiles)	return
		
		val oldOriginal	= entryList.removeFirst()
		val oldCached	= entryMap remove oldOriginal
		DEBUG("flushing original: " + oldOriginal)
		if (oldCached != null && oldCached.exists) {
			INFO("deleting cached: " +  oldCached)
			oldCached.delete()
		}
	}
	
	/** 
	  * delete stale entries from the entryList and entryMap
	  * and all cachefiles not in the entryMap and  
	  */
	private def cleanup() {
		// stale entries from the entryList and entryMap
		val originalFiles = new java.util.ArrayList[File](entryList)
		val it = originalFiles.iterator
		while (it.hasNext) {
			val original = it.next
			if (!original.exists) {
				WARN("original disappeared: " + original)
				entryList	remove original
				entryMap	remove original
			}
		}

		// delete all cachefiles not in the entryMap
		val entries	= entryMap.values
		val listed	= directory.listFiles	// TODO handle null
		for (cached <- listed) {
			if (!(entries contains cached)) {
				INFO("deleting cached: " + cached)
				cached.delete()
			}
		}
	}
	
	/** create a new cachefile */ 
	private def cacheFile():File = {
		while (true) {
			val name	= randomString("0123456789abcdefghijklmnopqrstuvwxyz", 14)
			val cached	= directory / name
			if (!cached.exists)	return cached
		}
		sys error "silence! i kill you!"
	}

	// TODO clone of StringUtil.randomString	
	/** create a random String from given characters */
	private def randomString(characters:String, length:Int):String = {
		val	chars	= characters.toCharArray
		val out		= new StringBuilder()
		for (i <- 0 until length) {
			val	index	= (java.lang.Math.random * chars.length).toInt
			val	c		= chars(index)
			out append c
		}
		return out.toString
	}
}
