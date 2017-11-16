package controllers.v1

import javax.inject.Singleton

import com.google.common.primitives.Primitives
import net.sf.ehcache.{Ehcache, Element}
import play.api.cache.CacheApi

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

@Singleton
class TestCache (cache: Ehcache) extends CacheApi {

  def set(key: String, value: Any, expiration: Duration) = {
    val element = new Element(key, value)
    expiration match {
      case infinite: Duration.Infinite => element.setEternal(true)
      case finite: FiniteDuration =>
        val seconds = finite.toSeconds
        if (seconds <= 0) {
          element.setTimeToLive(1)
        } else if (seconds > Int.MaxValue) {
          element.setTimeToLive(Int.MaxValue)
        } else {
          element.setTimeToLive(seconds.toInt)
        }
    }
    cache.put(element)
  }

  def get[T](key: String)(implicit ct: ClassTag[T]): Option[T] = {
    Option(cache.get(key)).map(_.getObjectValue).filter { v =>
      Primitives.wrap(ct.runtimeClass).isInstance(v) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v == ((): Unit))
    }.asInstanceOf[Option[T]]
  }

  def getOrElse[A: ClassTag](key: String, expiration: Duration)(orElse: => A) = {
    get[A](key).getOrElse {
      val value = orElse
      set(key, value, expiration)
      value
    }
  }

  def remove(key: String) = {
    cache.remove(key)
  }
}