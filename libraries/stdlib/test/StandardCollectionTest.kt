package test.collections

import kotlin.*
import kotlin.test.*
import junit.framework.TestCase

class StandardCollectionTest() : TestCase() {

  fun testDisabled() {
  }

  fun testAny() {
    val data: Iterable<String> = listOf("foo", "bar")

    assertTrue {
      data.any{it.startsWith("f")}
    }
    assertNot {
      data.any{it.startsWith("x")}
    }
  }
}
