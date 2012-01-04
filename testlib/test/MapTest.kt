package test.collections

import std.test.*

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import std.*
import std.io.*
import std.util.*
import java.util.*

class MapTest() : TestSupport() {
  val data: Map<String,Int> = HashMap<String, Int>()

  fun testGetOrElse() {
    val a = data.getOrElse("foo"){2}
    assertEquals(2, a)

    val b = data.getOrElse("foo"){3}
    assertEquals(3, b)
    assertEquals(0, data.size)
  }

  fun testGetOrElseUpdate() {
    val a = data.getOrElseUpdate("foo"){2}
    assertEquals(2, a)

    val b = data.getOrElseUpdate("foo"){3}
    assertEquals(2, b)

    assertEquals(1, data.size())
  }

  fun testSizeAndEmpty() {
    assert{ data.empty }

    assertEquals(data.size, 0)
  }
}