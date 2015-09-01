package commonist.thumb

import java.io._
import java.lang.{ Math => JMath }

import scala.collection.immutable.Queue

import scutil.implicits._
import scutil.io.Charsets._
import scutil.log._

/** caches derived Files indexed by their original Files */
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
		INFO("caching original", original)
		INFO("cached thumbnail", cached)
		entryQueue	= entryQueue enqueue original
		entryMap	= entryMap + (original -> cached)
		cached
	}

	/** remove an entry */
	def remove(original:File) {
		val cached	= entryMap get original
		DEBUG("removing original", original)
		entryQueue	= entryQueue filterNot { _ ==== original }
		entryMap	= entryMap - original
		cached filter { _.exists } foreach { cached =>
			INFO("deleting cached", cached)
			cached.delete()
		}
	}
	
	/** load cache metadata */
	def load() {
		INFO("loading")
		clear()
		if (!list.exists)	return
		
		DEBUG("reading metadata", list)
		try {
			list readLines utf_8 grouped 3 foreach { case Seq(s,o,c) =>
				require(s.isEmpty, "invalid separator")
				val original	= new File(o)
				val cached		= new File(c)
				entryQueue	= entryQueue enqueue original
				entryMap	= entryMap + (original -> cached)
			}
		}
		catch { case e:Exception	=>
			WARN("cannot load metadata cache", list, e)
		}
	}
	
	/** store cache metadata */
	def save() {
		INFO("saving")
		cleanup()

		DEBUG("writing metadata", list)
		val strs	= entryQueue map { original => s"\n${original.getPath}\n${entryMap(original).getPath}\n" }
		list writeString (utf_8, strs.mkString)
	}
	
	/** clear cache metadata */
	private def clear() {
		entryQueue	= Queue.empty
		entryMap	= Map.empty
	}
	
	/** remove the oldest cache entry and delete its file */
	private def flush() {
		if (entryQueue.nonEmpty && entryQueue.size > cachedFiles) {
			for {
				(oldOriginal,	newQueue)	<- entryQueue.extractHead
				(oldCached,		newMap)		<- entryMap extractAt oldOriginal
			} {
				entryQueue	= newQueue
				entryMap	= newMap
				DEBUG("flushing original", oldOriginal)
				if (oldCached.exists) {
					INFO("deleting cached", oldCached)
					oldCached.delete()
				}
			}
		}
	}
	
	/**
	  * delete stale entries from the entryList and entryMap
	  * and all cachefiles not in the entryMap and
	  */
	private def cleanup() {
		// remove stale entries from the entryList and entryMap
		entryQueue filterNot { _.exists } foreach { original =>
			WARN("original disappeared", original)
			entryQueue	= entryQueue filterNot { _ ==== original }
			entryMap	= entryMap - original
		}

		// delete all cachefiles not in the entryMap
		val entries	= entryMap.values.toSet
		val listed	= directory.children.flattenMany
		listed filterNot entries.contains foreach { cached =>
			INFO("deleting cached", cached)
			cached.delete()
		}
	}
	
	/** create a new cachefile */
	private def cacheFile():File = {
		val cached	= directory / randomString("0123456789abcdefghijklmnopqrstuvwxyz", 14)
		if (cached.exists)	cacheFile()
		else				cached
	}

	/** create a random String from given characters */
	private def randomString(characters:String, length:Int):String =
			0 until length map { _ => characters charAt (JMath.random * characters.length).toInt } mkString ""
}
