#带sbt的编译环境
#FROM registry.cn-hangzhou.aliyuncs.com/rang/sbt-builder AS builder
FROM registry.cn-shenzhen.aliyuncs.com/goldlok/sbt-builder AS builder
RUN mkdir -p /root/.sbt
RUN echo '[repositories] \n\
local\n\
my-maven: http://61.145.62.114:8078\n\
my-ivy: http://61.145.62.114:8078, [organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]\n\
' > /root/.sbt/repositories

RUN mkdir -p /root/workspace/agent
COPY . /root/workspace/agent
WORKDIR /root/workspace/agent
RUN set -ex && sbt assembly

FROM registry.cn-hangzhou.aliyuncs.com/aliware2018/services AS jars

FROM registry.cn-hangzhou.aliyuncs.com/aliware2018/debian-jdk8
COPY --from=jars /root/workspace/services/mesh-provider/target/mesh-provider-1.0-SNAPSHOT.jar /root/dists/mesh-provider.jar
COPY --from=jars /root/workspace/services/mesh-consumer/target/mesh-consumer-1.0-SNAPSHOT.jar /root/dists/mesh-consumer.jar
COPY --from=builder /root/workspace/agent/target/scala-2.12/mesh-agent.jar /root/dists/mesh-agent.jar

COPY --from=jars /usr/local/bin/docker-entrypoint.sh /usr/local/bin
COPY start-agent.sh /usr/local/bin
RUN chmod 777 /usr/local/bin/start-agent.sh

RUN set -ex && mkdir -p /root/logs

ENTRYPOINT ["docker-entrypoint.sh"]