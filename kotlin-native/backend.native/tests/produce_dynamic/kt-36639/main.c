#include "testlib_api.h"

#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x


int
main() {
  T_(CFoo1) foo1 = __ kotlin.root.CFoo1.CFoo1();
  T_(CFoo2) foo2 = __ kotlin.root.CFoo2.CFoo2();
  T_(kotlin_Int) intV = __ createNullableInt(42);
  T_(kotlin_Any) any;
  T_(Foo) foo = { .pinned = foo1.pinned };

  any.pinned = foo1.pinned;
  __ kotlin.root.CFoo1.callMe(foo1, any);
  __ kotlin.root.Foo.extfoo(foo, any);
  any.pinned = foo2.pinned;
  __ kotlin.root.CFoo1.callMe(foo1, any);
  __ kotlin.root.Foo.extfoo(foo, any);
  any.pinned = intV.pinned;
  __ kotlin.root.CFoo1.callMe(foo1, any);
  __ kotlin.root.Foo.extfoo(foo, any);

  __ DisposeStablePointer(foo1.pinned);
  __ DisposeStablePointer(foo2.pinned);
  __ DisposeStablePointer(intV.pinned);
}
