include($ENV{ZEPHYR_BASE}/cmake/app/boilerplate.cmake NO_POLICY_SCOPE)
project(NONE)

add_library(libsupcpp STATIC IMPORTED)

set_property(TARGET libsupcpp PROPERTY IMPORTED_LOCATION $ENV{GCCARMEMB_TOOLCHAIN_PATH}/arm-none-eabi/lib/thumb/libsupc++.a)

target_link_libraries(app libsupcpp)

set_property(TARGET app PROPERTY LINKER_LANGUAGE C)

target_sources(app PRIVATE build/program.o)

set_source_files_properties(program.o PROPERTIES EXTERNAL_OBJECT true GENERATED true)

add_custom_command(
  OUTPUT program.o
  COMMAND cp kotlin/program.o program.o
)
