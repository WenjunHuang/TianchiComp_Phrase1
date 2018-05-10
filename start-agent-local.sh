#!/bin/bash


# for local test
ETCD_HOST=192.168.2.248
ETCD_PORT=2379
AGENT_HOST=192.168.2.44
DUBBO_HOST=192.168.2.221
AGENT_PATH=/Users/xxzyjy/Sources/TianChi4thCompetition/target/scala-2.12/mesh-agent.jar

if [[ "$1" == "consumer" ]]; then
  echo "Starting consumer agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -DRUN_TYPE=consumer \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2551 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DHTTP_LISTENING_HOST=${AGENT_HOST} \
       -DHTTP_LISTENING_PORT=20000 \
       ${AGENT_PATH}
elif [[ "$1" == "provider-small" ]]; then
  echo "Starting small provider agent..."
  java -jar \
       -Xms512M \
       -Xmx512M \
       -DRUN_TYPE=provider-small \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2552 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${DUBBO_HOST} \
       -DDUBBO_PROVIDER_PORT=20889 \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       ${AGENT_PATH}
elif [[ "$1" == "provider-medium" ]]; then
  echo "Starting medium provider agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -DRUN_TYPE=provider-medium \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2553 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${DUBBO_HOST} \
       -DDUBBO_PROVIDER_PORT=20890 \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       ${AGENT_PATH}
elif [[ "$1" == "provider-large" ]]; then
  echo "Starting large provider agent..."
  java -jar \
       -Xms2560M \
       -Xmx2560M \
       -DRUN_TYPE=provider-large \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2554 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${DUBBO_HOST} \
       -DDUBBO_PROVIDER_PORT=20891 \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       ${AGENT_PATH}
else
  echo "Unrecognized arguments, exit."
  exit 1
fi