#!/bin/sh

# Pre-build hook: Apple Silicon support for libffi 3.3. Based on:
# * https://github.com/libffi/libffi/pull/565
# * https://github.com/libffi/libffi/pull/621

set -eu

PATCH_URL="https://raw.githubusercontent.com/Homebrew/formula-patches/06252df03c68aee70856e5842f85f20b259e5250/libffi/libffi-3.3-arm64.patch"
curl -L -o libffi-3.3-arm64.patch "$PATCH_URL"
patch -p1 --ignore-whitespace -i libffi-3.3-arm64.patch
