# Kotlin/Native toolchains builder

This directory contains a set of scripts and configuration files that allow one to build the same toolchain that is used by Kotlin/Native.

### System requirements
* Docker

### Usage
1. First, you need to build a Docker image with crosstool-ng inside. Use `create_image.sh` for it.
2. Now you can build an actual toolchain. To pick one, take a look inside `toolchains` folder. 
It is organized as `$TARGET/$VERSION`. Then run `./run_container.sh $TARGET $VERSION`. Building a toolchain might take a while (~17 minutes on Ryzen 5 3600). 
Once ready, an archive will be placed in `artifacts` folder.

### Example
```bash
./create_image.sh && ./run_container.sh aarch64-unknown-linux-gnu gcc-8.3.0-glibc-2.25-kernel-4.9
```

### Notes
Check that Docker can write to `artifacts` directory (where `build_toolchains.sh` places archives). 
