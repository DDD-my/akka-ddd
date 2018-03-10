package pl.newicom.dddd.messaging

import org.joda.time.DateTime
import org.joda.time.DateTime.now
import pl.newicom.dddd.messaging.MetaAttribute.{Id, Timestamp}
import pl.newicom.dddd.utils.UUIDSupport.uuid
import pl.newicom.dddd.utils.ImplicitUtils._

object MetaData {
  def empty: MetaData =
    new MetaData(Map.empty)

  def initial(id: String = uuid, timestamp: DateTime = now): MetaData =
    MetaData(Id -> id, Timestamp -> timestamp)

  def apply(attrs: (MetaAttribute[_], Any)*): MetaData =
    new MetaData(attrs.toMap.map(kv => kv._1.entryName -> kv._2))
}

case class MetaData(content: Map[String, Any]) extends Serializable {

  def withMetaData(metadata: MetaData): MetaData =
    copy(content = this.content ++ metadata.content)

  def withAttr[A](key: MetaAttribute[A], value: A): MetaData =
    copy(content = this.content + (key.entryName -> value))

  def withOptionalAttr[A](key: MetaAttribute[A], value: Option[A]): MetaData =
    value.map(withAttr(key, _)).getOrElse(this)

  def remove(attr: MetaAttribute[_]): MetaData =
    remove(attr.entryName)

  def remove(key: String): MetaData =
    copy(content = this.content - key)

  def contains(attrName: String): Boolean =
    content.contains(attrName)

  def contains(attr: MetaAttribute[_]): Boolean =
    tryGet(attr).isDefined

  def get[B](attrName: String): B =
    tryGet[B](attrName).get

  def get[B](attr: MetaAttribute[B]): B =
    tryGet(attr).get

  def tryGet[B](attrName: String): Option[B] =
    content.get(attrName).asParameterizedBy[B]

  def tryGet[B](attr: MetaAttribute[B]): Option[B] =
    content.get(attr.entryName).map(attr.read)

  override def toString: String =
    content.toString
}
