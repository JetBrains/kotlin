#!/bin/bash

set -e

if [ $# -ne 3 ]; then
  echo "This script creates and pushes a new git branch from a specified tag"
  echo "Usage: create_branch_from_tag.sh <url> <branch-name> <tag>"
  exit 1
fi

echo "Creating branch $2 from tag $3"

git fetch --no-tags $1 tag $3
git push origin $3^{commit}:refs/heads/$2