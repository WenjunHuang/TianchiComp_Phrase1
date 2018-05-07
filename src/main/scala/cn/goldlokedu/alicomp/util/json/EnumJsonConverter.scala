package cn.goldlokedu.alicomp.util.json

import java.util.Locale

import spray.json.{DeserializationException, JsNumber, JsString, JsValue, RootJsonFormat}

/**
  * 将枚举对象转换为整数
  *
  * @param enu 枚举对象
  * @tparam T 枚举类型
  */
class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue = JsNumber(obj.id)

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case JsNumber(id) => enu.apply(id.intValue())
      case somethingElse =>
        throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
    }
  }
}

/**
  * 将枚举对象转换为字符串
  *
  * @param enu 枚举对象
  * @tparam T 枚举类型
  */
class EnumNameJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue =
    JsString(obj.toString)

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case somethingElse =>
        throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
    }
  }
}

/**
  * 将枚举的名字（Pascal格式）与下划线格式（Snake格式）互换
  * @param enu 枚举对象
  * @tparam T 枚举类型
  */
class EnumNameSnakifyJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue = JsString(pascalToSnake(obj.toString))

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) =>
        enu.withName(txt)
      case somethingElse =>
        throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
    }
  }

  private val PASS1 = """([A-Z]+)([A-Z][a-z])""".r
  private val PASS2 = """([a-z\d])([A-Z])""".r
  private val REPLACEMENT = "$1_$2"
  private val PASS3 = """([A-Za-z\d])+""".r

  def pascalToSnake(name: String): String = PASS2.replaceAllIn(PASS1.replaceAllIn(name, REPLACEMENT), REPLACEMENT).toLowerCase(Locale.US)

  def snakeToPascal(name: String): String = PASS3.findAllIn(name).foldLeft("") { (accum, item) => accum + item.capitalize }
}
