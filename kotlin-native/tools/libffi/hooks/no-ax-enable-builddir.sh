#!/bin/sh
#
# Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
#
# Pre-build hook: remove AX_ENABLE_BUILDDIR.
#
# AX_ENABLE_BUILDDIR makes the build cd into a host-named directory and
# re-exec make there, which breaks Dockerized builds and cross builds
# (e.g. building the macos-x64 distribution on an Apple Silicon host,
# where the directory computed from `uname` does not match `--host`).
#
# Unlike docker-friendly-configure.sh this hook does NOT pin
# `sys_symbol_underscore`, so the native Mach-O probe stays intact.

set -eu

# `-i.bak` instead of plain `-i` to stay compatible with both GNU and BSD sed.
sed -i.bak '/AX_ENABLE_BUILDDIR/d' configure.ac
rm -f configure.ac.bak m4/ax_enable_builddir.m4

# Regenerate `configure` from the edited `configure.ac`.
if [ -f autogen.sh ]; then
    ./autogen.sh
else
    autoreconf -f -i
fi
