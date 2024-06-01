# SPDX-License-Identifier: Apache-2.0

cmake_minimum_required(VERSION 3.20.0)

find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})
project(kn_exp)

# set common paths
set(KN_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/..)
set(INCL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)
set(OUT_DIR__KN ${CMAKE_CURRENT_SOURCE_DIR}/out/kn)
set(SOURCE__KN ${CMAKE_CURRENT_SOURCE_DIR}/kotlin/src/nativeMain/kotlin)
set(SOURCE__NATIVE ${CMAKE_CURRENT_SOURCE_DIR}/native)
set(OBJ__KN_LIB_ORIGINAL ${OUT_DIR__KN}/libstatic.a.o)
set(OBJ__KN_LIB ${OUT_DIR__KN}/libkn.o)
set(OBJ__KN_HEADER__ORIGINAL ${CMAKE_CURRENT_SOURCE_DIR}/kotlin/build/bin/native/releaseShared/libkn_api.h)
set(INCL__KN_HEADER ${INCL_DIR}/libkn.h)

set(KOTLINC_NATIVE_BIN ${CMAKE_CURRENT_SOURCE_DIR}/../kotlin-native/dist/bin/kotlinc-native)

add_custom_command(
  OUTPUT ${OBJ__KN_LIB}
  COMMAND rm -rf ${OUT_DIR__KN}
  COMMAND ${KOTLINC_NATIVE_BIN} ${SOURCE__KN}/lib.kt -target zephyr_m55 -produce static -Xtemporary-files-dir=${OUT_DIR__KN} -module-name libkn
  COMMAND mv ${OBJ__KN_LIB_ORIGINAL} ${OBJ__KN_LIB}
  VERBATIM
)

# add_custom_command(
#   OUTPUT ${INCL__KN_HEADER}
#   COMMAND ${KN_ROOT}/gradlew linkNative
#   COMMAND mv ${OBJ__KN_HEADER__ORIGINAL} ${INCL__KN_HEADER}
#   WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/kotlin
#   VERBATIM
# )

add_library(kn_out ${OBJ__KN_LIB} ${INCL__KN_HEADER})

set_target_properties(kn_out PROPERTIES LINKER_LANGUAGE CXX)

# set_source_files_properties(
#     ${B} PROPERTIES
#     EXTERNAL_OBJECT TRUE  # Identifies this as an object file
#     GENERATED TRUE  # Avoids need for file to exist at configure-time
# )

# set(HEADER_FILES ${OUT_DIR__KN}/libkn.h)

target_include_directories(app PRIVATE ${INCL_DIR})
target_sources(app PRIVATE ${SOURCE__NATIVE}/main.cpp ${SOURCE__NATIVE}/polyfill.c)
target_link_libraries(app PUBLIC kn_out)
