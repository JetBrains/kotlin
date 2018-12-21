cmake_minimum_required(VERSION 3.8.2)

include($ENV{ZEPHYR_BASE}/cmake/app/boilerplate.cmake NO_POLICY_SCOPE)
project(NONE)

add_library(libsupcpp STATIC IMPORTED)

set_property(TARGET libsupcpp PROPERTY IMPORTED_LOCATION $ENV{GNUARMEMB_TOOLCHAIN_PATH}/arm-none-eabi/lib/thumb/libsupc++.a)

target_link_libraries(app PRIVATE libsupcpp)

set_property(TARGET app PROPERTY LINKER_LANGUAGE C)

target_sources(app PRIVATE build/program.o)

set_source_files_properties(program.o PROPERTIES EXTERNAL_OBJECT true GENERATED true)

add_custom_command(
  OUTPUT program.o
  COMMAND ${CMAKE_COMMAND} -E copy
    ${CMAKE_BINARY_DIR}/kotlin/program.o
    ${CMAKE_BINARY_DIR}/program.o
)
