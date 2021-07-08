#!/bin/bash
set -e
repo="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

iters=$1

for i in $(seq 1 $iters); do
    echo $i
    "$repo/run.sh" 16 || exit 1
done
