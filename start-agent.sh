#!/bin/bash

# production
ETCD_HOST=etcd
ETCD_PORT=2379
DUBBO_PORT=20880
AGENT_PATH=/root/dists/mesh-agent.jar

if [[ "$1" == "consumer" ]]; then
  echo "Starting consumer agent..."
  java -jar \
       -server \
       -Xms1536M \
       -Xmx1536M \
       -DRUN_TYPE=consumer \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DHTTP_LISTENING_PORT=20000 \
       ${AGENT_PATH}
elif [[ "$1" == "provider-small" ]]; then
  echo "Starting small provider agent..."
  java -jar \
       -Xms512M \
       -Xmx512M \
       -DRUN_TYPE=provider-small \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_PORT=${DUBBO_PORT} \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       -DPROVIDER_AGENT_HOST=provider-small \
       ${AGENT_PATH}
elif [[ "$1" == "provider-medium" ]]; then
  echo "Starting medium provider agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -DRUN_TYPE=provider-medium \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_PORT=${DUBBO_PORT} \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       -DPROVIDER_AGENT_HOST=provider-medium \
       ${AGENT_PATH}
elif [[ "$1" == "provider-large" ]]; then
  echo "Starting large provider agent..."
  java -jar \
       -Xms2560M \
       -Xmx2560M \
       -DRUN_TYPE=provider-large \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_PORT=${DUBBO_PORT} \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=200 \
       -DPROVIDER_AGENT_HOST=provider-large \
       ${AGENT_PATH}
else
  echo "Unrecognized arguments, exit."
  exit 1
fi