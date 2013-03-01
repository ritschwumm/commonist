package commonist.thumb

import java.io._
import java.lang.{ Math => JMath }

import scala.collection.immutable.Queue

import scutil.Implicits._
import scutil.Resource._
import scutil.log.Logging

/** caches drived Files indexed by their original Files */
final class FileCache(list:File, directory:File, cachedFiles:Int) extends Logging {
	directory.mkdirs()
	
	var entryQueue:Queue[File]	= Queue.empty
	var entryMap:Map[File,File]	= Map.empty

	/** get a cachefile if it exists */
	def get(original:File):Option[File] = {
		entryMap get original flatMap { cached =>
			if (original newerThan cached) {
				remove(original)
				None
			} 
			else {
				// move to the end of the list (LRU)
				entryQueue	= entryQueue filterNot { _ ==== original }
				entryQueue	= entryQueue enqueue original
				Some(cached)
			}
		}
	}
	
	/** create a new cachefile */
	def put(original:File):File = {
		flush()
		
		// insert a new entry
		val	cached	= cacheFile()
		INFO("caching original: " + original)
		INFO("cached thumbnail: " + cached)
		entryQueue	= entryQueue enqueue original
		entryMap	= entryMap + (original -> cached)
		cached
	}

	/** remove an entry */
	def remove(original:File) {
		val cached	= entryMap get original
		DEBUG("removing original: " + original)
		entryQueue	= entryQueue filterNot { _ ==== original }
		entryMap	= entryMap - original
		cached filter { _.exists } foreach { cached =>
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
						entryQueue	= entryQueue enqueue original
						entryMap	= entryMap + (original -> cached)
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
			entryQueue foreach { original =>
				val cached		= entryMap apply original
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
		entryQueue	= Queue.empty
		entryMap	= Map.empty
	}
	
	/** remove the oldest cache entry and delete its file */
	private def flush() {
		if (entryQueue.isEmpty || entryQueue.size <= cachedFiles)	return
		
		for {
			(oldOriginal,newQueue)	<- entryQueue.dequeueOption
			(oldCached,newMap)		<- extractOption(entryMap, oldOriginal)
		} {
			entryQueue	= newQueue
			entryMap	= newMap
			DEBUG("flushing original: " + oldOriginal)
			if (oldCached.exists) {
				INFO("deleting cached: " +  oldCached)
				oldCached.delete()
			}
		}
	}
	
	// TODO use scutil extension when available
	private def extractOption[S,T](map:Map[S,T], key:S):Option[(T,Map[S,T])]	=
			map get key map { value => (value, map - key) }
	
	/** 
	  * delete stale entries from the entryList and entryMap
	  * and all cachefiles not in the entryMap and  
	  */
	private def cleanup() {
		// stale entries from the entryList and entryMap
		entryQueue foreach { original =>
			if (!original.exists) {
				WARN("original disappeared: " + original)
				entryQueue	= entryQueue filterNot { _ ==== original }
				entryMap	= entryMap - original
			}
		}

		// delete all cachefiles not in the entryMap
		val entries	= entryMap.values.toSet
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

	/** create a random String from given characters */
	private def randomString(characters:String, length:Int):String =
			0 until length map { _ => characters charAt (JMath.random * characters.length).toInt } mkString ""
}
