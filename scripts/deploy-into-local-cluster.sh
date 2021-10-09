#!/usr/bin/env bash

set -euxo

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

usage() {
    cat <<EOM
    Usage: $(basename $0) -v|--version version

EOM
    exit 0
}

while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
    -v|--version)
      VERSION="$2"
      shift
      shift
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z ${VERSION+x} ]; then
  usage
fi

kubectl --context kind-spottle --namespace spottle scale deployment spottle-engine --replicas 0 || true
kubectl --context kind-spottle --namespace spottle scale deployment spottle-edge --replicas 0 || true

kind load docker-image "spottle-engine:${VERSION}" --name spottle
kind load docker-image "spottle-edge:${VERSION}" --name spottle

CHART_DIR="${SCRIPT_DIR}/../spottle-deployment"

helm upgrade spottle  "${CHART_DIR}" \
    --install \
    --namespace spottle \
    --values "${CHART_DIR}/values.yaml" \
    --set "engine.version=${VERSION},edge.version=${VERSION}"

kubectl wait \
    --timeout=60s \
    --namespace spottle \
    --selector=app=spottle-engine \
    --for=condition=ready \
    pod

kubectl wait \
    --timeout=60s \
    --namespace spottle \
    --selector=app=spottle-edge \
    --for=condition=ready \
    pod
