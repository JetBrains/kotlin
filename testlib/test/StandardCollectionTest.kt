package test.collections

import std.*
import std.test.*

class StandardCollectionTest() : TestSupport() {

  fun testAny() {
    // TODO is a cast really required?
    // doesn't compile without it, see KT-924
    val data: Iterable<String> = std.util.arrayList("foo", "bar") as Iterable<String>

    assert {
      data.any{it.startsWith("f")}
    }
    assertNot {
      data.any{it.startsWith("x")}
    }
  }
}