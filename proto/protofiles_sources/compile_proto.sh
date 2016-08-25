#!/bin/bash
PROTOC="../compiler/build/protoc"
cd "$(dirname "${BASH_SOURCE}")"
mkdir out -p
${PROTOC} --kotlin_out="./out/" --proto_path="./server_car" ./server_car/*.proto
${PROTOC} --kotlin_out="./out/" --proto_path="./server_client" ./server_client/*.proto
