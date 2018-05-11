package cn.goldlokedu.alicomp.documents

import akka.util.ByteString

case class ConsumerEncodeBenchmarkRequest(requestId:Long,msg:ByteString)
