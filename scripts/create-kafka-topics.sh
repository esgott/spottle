#!/usr/bin/env bash

set -euxo

kafka-topics --bootstrap-server localhost:9092 --create --topic spottle.commands.v1
kafka-topics --bootstrap-server localhost:9092 --create --topic spottle.events.v1
