#!/usr/bin/env bash

set -euxo

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

kind create cluster \
    --name spottle \
    --config "${SCRIPT_DIR}/kind-config.yaml" \
    --wait 5m

kubectl --context kind-spottle create namespace kafka
kubectl --context kind-spottle create namespace spottle

kubectl apply \
    --filename https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

kubectl apply \
    --context kind-spottle \
    --namespace kafka \
    --filename "${SCRIPT_DIR}/kafka-deployment.yaml"

kubectl wait \
    --timeout=300s \
    --namespace ingress-nginx \
    --selector=app.kubernetes.io/component=controller \
    --for=condition=ready \
    pod

kubectl wait \
    --timeout=180s \
    --namespace kafka \
    --selector=app=kafka \
    --for=condition=ready \
    pod

kafka-topics --bootstrap-server localhost:9092 --create --topic spottle.commands.v1
kafka-topics --bootstrap-server localhost:9092 --create --topic spottle.events.v1
