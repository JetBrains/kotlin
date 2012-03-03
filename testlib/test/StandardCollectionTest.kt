package test.collections

import kotlin.*
import kotlin.test.*
import junit.framework.TestCase

class StandardCollectionTest() : TestCase() {

  fun testDisabled() {
  }

  /*
  fun testAny() {
    // TODO requires KT-924 to be implemented
    val data: Iterable<String> = kotlin.util.arrayList("foo", "bar")

    assertTrue {
      data.any{it.startsWith("f")}
    }
    assertNot {
      data.any{it.startsWith("x")}
    }
  }
  */
}
