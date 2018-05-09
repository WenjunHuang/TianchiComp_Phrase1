#!/bin/bash

HOST=$(ip addr show docker0 | grep 'inet\b' | awk '{print $2}' | cut -d '/' -f 1)
ETCD_PORT=2379
ETCD_URL=http://${ETCD_HOST}:${ETCD_PORT}
echo ETCD_URL = ${ETCD_URL}

if [[ "$1" == "consumer" ]]; then
  echo "Starting consumer agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -DTYPE=consumer \
       -DACTOR_SYSTEM_HOST=${HOST} \
       -DACTOR_SYSTEM_PORT=2551 \
       -DETCD_HOST=${HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DHTTP_LISTENING_HOST=${HOST} \
       -DHTTP_LISTENING_PORT=2000 \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-small" ]]; then
  echo "Starting small provider agent..."
  java -jar \
       -Xms512M \
       -Xmx512M \
       -DRUN_TYPE=provider-small \
       -DACTOR_SYSTEM_HOST=${HOST} \
       -DACTOR_SYSTEM_PORT=2552 \
       -DETCD_HOST=${HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${HOST} \
       -DDUBBO_PROVIDER_PORT=20889 \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-medium" ]]; then
  echo "Starting medium provider agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -DRUN_TYPE=provider-medium \
       -DACTOR_SYSTEM_HOST=${HOST} \
       -DACTOR_SYSTEM_PORT=2553 \
       -DETCD_HOST=${HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${HOST} \
       -DDUBBO_PROVIDER_PORT=20890 \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-large" ]]; then
  echo "Starting large provider agent..."
  java -jar \
       -Xms2560M \
       -Xmx2560M \
       -DRUN_TYPE=provider-large \
       -DACTOR_SYSTEM_HOST=${HOST} \
       -DACTOR_SYSTEM_PORT=2554 \
       -DETCD_HOST=${HOST} \
       -DETCD_PORT=${ETCD_PORT} \
       -DDUBBO_PROVIDER_HOST=${HOST} \
       -DDUBBO_PROVIDER_PORT=20891 \
       /root/dists/mesh-agent.jar
else
  echo "Unrecognized arguments, exit."
  exit 1
fi