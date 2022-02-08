#include "testlib_api.h"

#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x


int
main() {
#if TEST == 1
   __ kotlin.root.empty.nonempty.one();
#endif
  return 0;
}
