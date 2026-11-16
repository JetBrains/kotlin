#!/bin/sh

# Pre-build hook: make libffi configure usable inside a Dockerized build.
#
# * AX_ENABLE_BUILDDIR makes the build cd into a host-named directory and
#   re-exec make, which breaks Dockerized builds.
# * LT_SYS_SYMBOL_USCORE probes for the leading underscore by linking AND
#   running a test program, which cannot work while cross-compiling. Pin
#   the value a native build would detect: PE symbols on x64 have no
#   leading underscore, neither do ELF symbols on x86-64 and aarch64.

set -eu

# `-i.bak` instead of plain `-i` to stay compatible with both GNU and BSD sed.
sed -i.bak '/AX_ENABLE_BUILDDIR/d' configure.ac
sed -i.bak 's/^LT_SYS_SYMBOL_USCORE$/sys_symbol_underscore=no/' configure.ac
rm -f configure.ac.bak m4/ax_enable_builddir.m4

# Regenerate `configure` from the edited `configure.ac`.
if [ -f autogen.sh ]; then
    ./autogen.sh
else
    autoreconf -f -i
fi
