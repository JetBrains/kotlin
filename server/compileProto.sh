#!/bin/bash
../proto/compiler/google/src/google/protobuf/compiler/kotlin/protoc --kotlin_out="./src/main/java" --proto_path="../proto/server_car" ../proto/server_car/*.proto
