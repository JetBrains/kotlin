FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

ENV TZ=Etc/UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc-mingw-w64-x86-64 autoconf automake libtool make texinfo git curl ca-certificates file \
  && rm -rf /var/lib/apt/lists/*

ARG LIBFFI_VERSION=3.3
ARG REVISION=1
WORKDIR /build

# Has to match with `predefinedLibffiVersions` and `libffiDir.mingw_x64` in konan.properties.
RUN echo "libffi-${LIBFFI_VERSION}-windows-x64-${REVISION}" > artifact.name

RUN git clone https://github.com/libffi/libffi.git \
  && cd libffi \
  && git checkout v${LIBFFI_VERSION}

WORKDIR /build/libffi
# The same two configure.ac patches as images/linux/Dockerfile:
# * AX_ENABLE_BUILDDIR makes the build cd into a host-named directory and
#   re-exec make, which breaks Dockerized builds.
# * LT_SYS_SYMBOL_USCORE probes for the leading underscore by linking AND
#   running a test program, which cannot work while cross-compiling. Pin the
#   value a native x86_64-w64-mingw32 build would detect: x64 PE symbols have
#   no leading underscore.
RUN sed -i '/AX_ENABLE_BUILDDIR/d' configure.ac \
  && rm -f m4/ax_enable_builddir.m4 \
  && sed -i 's/^LT_SYS_SYMBOL_USCORE$/sys_symbol_underscore=no/' configure.ac
RUN ./autogen.sh \
  && NAME="$(cat ../artifact.name)" \
  && ./configure --host=x86_64-w64-mingw32 --prefix="/build/$NAME" --disable-docs --enable-shared=no \
  && make -j"$(nproc)" \
  && make install

WORKDIR /build

RUN NAME="$(cat artifact.name)"; \
    rm -rf "$NAME/lib/pkgconfig" "$NAME/share" \
  && tar -czf "$NAME.tar.gz" "$NAME"

CMD ["sh", "-c", "NAME=\"$(cat /build/artifact.name)\"; file \"/build/$NAME/lib/libffi.a\" && test -f \"/build/$NAME/include/ffi.h\" && tar -tzf \"/build/$NAME.tar.gz\" | head -5"]
