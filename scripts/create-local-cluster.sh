#!/usr/bin/env bash

set -euxo

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

kind create cluster \
    --name spottle \
    --config "${SCRIPT_DIR}/kind-config.yaml" \
    --wait 5m

kubectl --context kind-spottle create namespace spottle
kubectl --context kind-spottle create namespace kafka

kubectl --context kind-spottle --namespace kafka apply -f "${SCRIPT_DIR}/kafka-deployment.yaml"
