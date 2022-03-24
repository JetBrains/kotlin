cmake_minimum_required(VERSION 3.20)
project(libllvmext)

set(CMAKE_CXX_STANDARD 17)

function(find_llvm_dir)
    message(STATUS "Loading LLVM dependency location from Gradle...")
    # TODO: Check on Windows.
    execute_process(
            COMMAND ../../gradlew printLlvmDir -q
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            OUTPUT_VARIABLE llvm_dir
            OUTPUT_STRIP_TRAILING_WHITESPACE
    )
    set(LLVM_DIR ${llvm_dir} PARENT_SCOPE)
endfunction(find_llvm_dir)

if (NOT DEFINED LLVM_DIR)
    find_llvm_dir()
endif ()
if (LLVM_DIR STREQUAL "")
    message(FATAL_ERROR "LLVM_DIR is not set!")
else()
    message(STATUS "LLVM location: ${LLVM_DIR}")
endif()
list(APPEND CMAKE_PREFIX_PATH "${LLVM_DIR}/lib/cmake")
find_package(LLVM REQUIRED CONFIG)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

if (CMAKE_SYSTEM_NAME MATCHES Darwin)
    add_compile_definitions(KONAN_MACOS=1)
elseif (CMAKE_SYSTEM_NAME MATCHES Linux)
    add_compile_definitions(KONAN_LINUX=1)
elseif (CMAKE_SYSTEM_NAME MATCHES Windows)
    add_compile_definitions(KONAN_WINDOWS=1)
endif ()

set(SRC_DIR ${CMAKE_SOURCE_DIR}/src)
include_directories(${LLVM_INCLUDE_DIRS})
include_directories(${SRC_DIR}/main/include)

add_library(libllvmext ${SRC_DIR}/main/cpp/CoverageMappingC.cpp ${SRC_DIR}/main/cpp/CAPIExtensions.cpp)
