package controllers.v1

import net.sf.ehcache.{CacheManager, Element}
import play.api.cache.CacheApi

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

// https://stackoverflow.com/questions/39453838/play-scala-2-5-testing-classes-injecting-cache-leads-to-an-error
class TestCache extends CacheApi {
  lazy val cache = {
    val manager = CacheManager.getInstance()
    manager.addCacheIfAbsent("play")
    manager.getCache("play")
  }

  def set(key: String, value: Any, expiration: Duration = Duration.Inf) = cache.put(new Element(key, value))

  def remove(key: String) = cache.remove(key)

  def getOrElse[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => A): A = {
    get[A](key).getOrElse {
      val value = orElse
      set(key, value, expiration)
      value
    }
  }

  def get[T: ClassTag](key: String): Option[T] = {
    Try(cache.get(key).getObjectValue.asInstanceOf[T]) match {
      case Success(s) => Some(s)
      case Failure(ex) => None
    }
  }
}
