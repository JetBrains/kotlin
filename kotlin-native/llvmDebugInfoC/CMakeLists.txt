cmake_minimum_required(VERSION 3.26)
project(llvmDebugInfoC)

set(CMAKE_CXX_STANDARD 14)

include_directories(src/main/include)
include_directories(/Users/Timofey.Solonin/.konan/dependencies/apple-llvm-20200714-macos-aarch64-1/include/)

add_executable(llvmDebugInfoC
        src/main/cpp/DebugInfoC.cpp
        src/main/include/DebugInfoC.h)
