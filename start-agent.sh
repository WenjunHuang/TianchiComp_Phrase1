#!/bin/bash

# production
HOST=$(ip addr show docker0 | grep 'inet\b' | awk '{print $2}' | cut -d '/' -f 1)
ETCD_HOST=${HOST}
ETCD_PORT=2379
AGENT_HOST=${HOST}
DUBBO_HOST=${HOST}
AGENT_PATH=/root/dists/mesh-agent.jar

if [[ "$1" == "consumer" ]]; then
  echo "Starting consumer agent..."
  java -jar \
       -server \
       -Xms1536M \
       -Xmx1536M \
       -XX:+UseNUMA \
       -XX:+UseCondCardMark \
       -XX:-UseBiasedLocking \
       -DRUN_TYPE=consumer \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2551 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DHTTP_LISTENING_PORT=20000 \
       ${AGENT_PATH}
elif [[ "$1" == "provider-small" ]]; then
  echo "Starting small provider agent..."
  java -jar \
       -Xms512M \
       -Xmx512M \
       -XX:+UseNUMA \
       -XX:+UseCondCardMark \
       -XX:-UseBiasedLocking \
       -DRUN_TYPE=provider-small \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2552 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${DUBBO_HOST} \
       -DDUBBO_PROVIDER_PORT=20889 \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       -DPROVIDER_AGENT_HOST=${AGENT_HOST} \
       ${AGENT_PATH}
elif [[ "$1" == "provider-medium" ]]; then
  echo "Starting medium provider agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -XX:+UseNUMA \
       -XX:+UseCondCardMark \
       -XX:-UseBiasedLocking \
       -DRUN_TYPE=provider-medium \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2553 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${DUBBO_HOST} \
       -DDUBBO_PROVIDER_PORT=20890 \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       -DPROVIDER_AGENT_HOST=${AGENT_HOST} \
       ${AGENT_PATH}
elif [[ "$1" == "provider-large" ]]; then
  echo "Starting large provider agent..."
  java -jar \
       -Xms2560M \
       -Xmx2560M \
       -XX:+UseNUMA \
       -XX:+UseCondCardMark \
       -XX:-UseBiasedLocking \
       -DRUN_TYPE=provider-large \
       -DACTOR_SYSTEM_HOST=${AGENT_HOST} \
       -DACTOR_SYSTEM_PORT=2554 \
       -DETCD_HOST=${ETCD_HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${DUBBO_HOST} \
       -DDUBBO_PROVIDER_PORT=20891 \
       -DDUBBO_CONNECTION_COUNT=2 \
       -DDUBBO_COUNT_PER_CONNECTION=100 \
       -DPROVIDER_AGENT_HOST=${AGENT_HOST} \
       ${AGENT_PATH}
else
  echo "Unrecognized arguments, exit."
  exit 1
fi