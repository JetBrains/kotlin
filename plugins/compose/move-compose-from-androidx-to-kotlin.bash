#!/bin/bash

#
# Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

set -e # Exit if one of commands exit with non-zero exit code
set -u # Treat unset variables and parameters other than the special parameters ‘@’ or ‘*’ as an error
set -x # Print commands and their output

if [ $# -ne 2 ]; then
    echo "Usage cherry-pick-compose-from-androidx-to-kotlin /path/to/androidx androidxSince androidxUntil destinationDir"
    echo "! Script should be started from the directory with the destination repository !"
    echo "  androidxPath         - path to the androidx repository"
    echo "  androidxUntil        - androidx until commit (including)"
    exit 1
fi

kotlinComposeRepo="$(pwd)"
androidxRepo="$1"

# Last update:
since=4722294b # Resulting commit in the Kotlin repo

echo "--- First phase. Moving compose files to new positions and deleting other unrelated files."

cd $androidxRepo
androidxUntil=$(git rev-parse $2)

git checkout "androidx-main"
git branch -f "move-branch" $androidxUntil
git checkout "move-branch"

# git fetch origin "move-branch"

# Prepare parts on androidx for moving
#  - Leave only relevant directories with commits rewrite
#  - Rewrite commits
#    - Add information about origin commit
git-filter-repo \
  --path compose/plugins/cli/ --path-rename compose/plugins/cli/:plugins/cli/ \
  --path compose/plugins/cli-tests/ --path-rename compose/plugins/cli-tests/:plugins/cli-tests/ \
  --path compose/compose-compiler-hosted/ --path-rename compose/compose-compiler-hosted/:compose-compiler-hosted/ \
  --path compose/compiler/ --path-rename compose/compiler/:plugins/compose/ \
  --preserve-commit-hashes \
  --commit-callback '

  def replace_bug_pattern(prefix, input_bytes):
      import re
      input_string = input_bytes.decode('\''utf-8'\'')
      pattern = f'\''{prefix}(\d+)'\''
      def replacement(match):
          return match.group(0) + " ( https://issuetracker.google.com/issues/" + match.group(1) + " )"
      return re.sub(pattern, replacement, input_string).encode('\''utf-8'\'')

  def replace_changeid_pattern(input_bytes):
      import re
      input_string = input_bytes.decode('\''utf-8'\'')
      pattern = r'\''I[a-fA-F0-9]{40}'\''
      def replacement(match):
          return match.group(0) + '\'' ( https://android-review.googlesource.com/q/'\'' + match.group(0) + '\'' )'\''
      replaced_text = re.sub(pattern, replacement, input_string)
      return replaced_text.encode('\''utf-8'\'')

  commit.message = replace_bug_pattern("Bug: ", commit.message)
  commit.message = replace_bug_pattern("Fixes: ", commit.message)
  commit.message = replace_bug_pattern("Issue: ", commit.message)
  commit.message = replace_bug_pattern("bug: ", commit.message)
  commit.message = replace_bug_pattern("fixes: ", commit.message)
  commit.message = replace_bug_pattern("issue: ", commit.message)
  commit.message = replace_bug_pattern("b/", commit.message)
  commit.message = replace_changeid_pattern(commit.message)

  commit.message += b"\nMoved from: https://github.com/androidx/androidx/commit/%s" % commit.original_id

    ' \
  --force

echo "--- Second phase. Merge everything to the destination repository"
cd $kotlinComposeRepo

git remote remove androidx || true
git remote add "androidx" $androidxRepo

git fetch "androidx" "move-branch"
echo "--- Update to commit"
git log --pretty=format:'%h' -n 1 androidx/move-branch

git cherry-pick $since..androidx/move-branch -m 1