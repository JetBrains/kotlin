FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

ENV TZ=Etc/UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential git autoconf automake libtool texinfo curl ca-certificates file \
 && rm -rf /var/lib/apt/lists/*

ARG LIBFFI_VERSION=3.2.1
ARG REVISION=2
WORKDIR /build

RUN set -eux; \
    case "$(uname -m)" in \
      x86_64)  arch=x86-64 ;; \
      aarch64) arch=arm64 ;; \
      *) echo "unsupported arch: $(uname -m)" >&2; exit 1 ;; \
    esac; \
    echo "libffi-${LIBFFI_VERSION}-${REVISION}-linux-${arch}" > artifact.name

# Pull libffi from GitHub and checkout to the proper branch-tag
RUN git clone https://github.com/libffi/libffi.git \
 && cd libffi \
 && git checkout v${LIBFFI_VERSION}

WORKDIR /build/libffi
RUN sed -i '/AX_ENABLE_BUILDDIR/d' configure.ac \
 && rm -f m4/ax_enable_builddir.m4 \
 && sed -i 's/^LT_SYS_SYMBOL_USCORE$/sys_symbol_underscore=no/' configure.ac
RUN ./autogen.sh \
 && NAME="$(cat ../artifact.name)" \
 && ./configure --prefix="/build/$NAME" --disable-docs --enable-shared=no \
 && make -j"$(nproc)" \
 && make install

WORKDIR /build

# Build libffi within the container
RUN NAME="$(cat artifact.name)"; \
    mv "$NAME/lib/libffi-${LIBFFI_VERSION}/include" "$NAME/include" \
 && rm -rf "$NAME/lib/libffi-${LIBFFI_VERSION}" "$NAME/lib/pkgconfig" "$NAME/share" \
 && tar -czf "$NAME.tar.gz" "$NAME"

# Verification check (if the file exists as the Native compile is expecred
CMD ["sh", "-c", "NAME=\"$(cat /build/artifact.name)\"; file \"/build/$NAME/lib/libffi.a\" && test -f \"/build/$NAME/include/ffi.h\" && tar -tzf \"/build/$NAME.tar.gz\" | head -5"]
