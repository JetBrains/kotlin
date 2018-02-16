include($ENV{ZEPHYR_BASE}/cmake/app/boilerplate.cmake NO_POLICY_SCOPE)
project(NONE)

add_library(libsupcpp STATIC IMPORTED)

set_property(TARGET libsupcpp  PROPERTY IMPORTED_LOCATION $ENV{GCCARMEMB_TOOLCHAIN_PATH}/arm-none-eabi/lib/thumb/libsupc++.a )

target_link_libraries(app libsupcpp)

target_sources(app PRIVATE src/empty.c build/program.o )
