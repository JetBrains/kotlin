#!/bin/bash
../compiler/build/protoc --kotlin_out="./out/" --proto_path="./server_car" ./server_car/*.proto
