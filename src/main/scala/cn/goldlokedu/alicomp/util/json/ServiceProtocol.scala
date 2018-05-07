package cn.goldlokedu.alicomp.util.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

object ServiceProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  import reflect._

  private val PASS1 = """([A-Z]+)([A-Z][a-z])""".r
  private val PASS2 = """([a-z\d])([A-Z])""".r
  private val REPLACEMENT = "$1_$2"

  override protected def extractFieldNames(classTag: ClassTag[_]): Array[String] = {
    import java.util.Locale
    def snakify(name: String) = PASS2.replaceAllIn(PASS1.replaceAllIn(name, REPLACEMENT), REPLACEMENT).toLowerCase(Locale.US)

    super.extractFieldNames(classTag) map snakify
  }

  def toJsonBytes[T: JsonFormat]: T ⇒ Array[Byte] = t ⇒
    t.toJson.compactPrint.getBytes("utf-8")

  def fromJsonBytes[T: JsonFormat]: Array[Byte] ⇒ T = bytes ⇒
    new String(bytes, "utf-8").parseJson.convertTo[T]
}