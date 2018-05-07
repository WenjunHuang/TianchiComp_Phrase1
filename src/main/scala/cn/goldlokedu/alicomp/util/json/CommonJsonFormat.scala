package cn.goldlokedu.alicomp.util.json

import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

class CommonJsonFormat[T, U](createFunc: (U) => T, valueFunc: (T) => U)(implicit str2U: (String) => U) extends RootJsonFormat[T] {
  override def write(obj: T) = JsString(valueFunc(obj).toString)
  override def read(json: JsValue): T = json match {
    case JsString(str) => createFunc(str)
    case otherValue => throw DeserializationException(s"$this value not found ind ${otherValue.toString}")
  }
}
