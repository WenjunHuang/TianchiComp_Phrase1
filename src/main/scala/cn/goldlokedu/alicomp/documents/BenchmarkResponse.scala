package cn.goldlokedu.alicomp.documents

import akka.http.scaladsl.common.EntityStreamingSupport

import scala.util.Try

case class BenchmarkResponse(requestId: Long,
                             status: Int,
                             result: Option[Int])

object BenchmarkResponse {
  def apply(message: DubboMessage): Try[BenchmarkResponse] = {
    Try {
      // 测试的结果是个32位整数值，但是dubbo用fastjson编码后得出的是一个字符串，例如 9900，结果是"1\n9900\n"字符串
      val raw = message.body.drop(2).dropRight(1) // 头两个字节是"1\n",最后一个字节是"\n"
      val status = message.status.toInt
      var result: Option[Int] = None

      if (status == 20) {
        if (raw.head == 45){
          // 负数
          val r = raw.drop(1).foldLeft(0) { (accum, it) =>
            accum * 10 + (it - 48) // 数字的ascii码-48=数字值
          }
          result = Some(-r)
        }else {
          // 正数
          result = Some(raw.foldLeft(0) { (accum, it) =>
            accum * 10 + (it - 48) // 数字的ascii码-48=数字值
          })
        }
      }
      BenchmarkResponse(message.requestId, status, result)
    }
  }
}

