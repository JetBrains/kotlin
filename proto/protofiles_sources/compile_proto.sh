#!/bin/bash
parent_path=$( cd "$(dirname "${BASH_SOURCE}")" ; pwd -P )
cd "$parent_path"
mkdir out -p
../compiler/build/protoc --kotlin_out="./out/" --proto_path="./server_car" ./server_car/*.proto
