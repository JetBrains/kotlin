#!/usr/bin/env python3
#
# Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license

SOURCES = {
    "3.2.1": {
        "url": "https://github.com/libffi/libffi/archive/refs/tags/v3.2.1.tar.gz",
        "directory": "libffi-3.2.1",
    },
    "3.3": {
        "url": "https://github.com/libffi/libffi/releases/download/v3.3/libffi-3.3.tar.gz",
        "directory": "libffi-3.3",
    },
}

# Supported build targets. `distribution` must match `libffiDir.*` in
# `konan.properties`, and must be listed in `predefinedLibffiVersions`.
TARGETS = {
    "macos-x64": {
        "version": "3.2.1",
        "distribution": "libffi-3.2.1-3-darwin-macos",
        "hooks": ["hooks/no-ax-enable-builddir.sh"],
        "configure_host": "x86_64-apple-darwin",
    },
    "macos-arm64": {
        "version": "3.3",
        "distribution": "libffi-3.3-1-macos-arm64",
        "hooks": ["hooks/macos-arm64-support.sh"],
    },
    "linux-x86-64": {
        "version": "3.2.1",
        "distribution": "libffi-3.2.1-2-linux-x86-64",
        "hooks": ["hooks/docker-friendly-configure.sh"],
    },
    "linux-arm64": {
        "version": "3.2.1",
        "distribution": "libffi-3.2.1-2-linux-arm64",
        "hooks": ["hooks/docker-friendly-configure.sh"],
    },
    "mingw-x64": {
        "version": "3.3",
        "distribution": "libffi-3.3-windows-x64-1",
        "hooks": ["hooks/docker-friendly-configure.sh"],
        "configure_host": "x86_64-w64-mingw32",
    },
}
