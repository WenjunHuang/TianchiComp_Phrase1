package cn.goldlokedu.alicomp.documents

import java.nio.ByteOrder

import akka.util.ByteString

case class DubboMessage(isRequest: Boolean,
                        is2Way: Boolean,
                        isEvent: Boolean,
                        serializationId: Byte,
                        status: Byte,
                        requestId: Long,
                        dataLength: Int,
                        body: ByteString) {
  def isResponse: Boolean = !isRequest && !isEvent
}

