package test.collections

import stdhack.test.*

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import std.*
import std.io.*
import std.util.*
import java.util.*

class MapTest() : TestSupport() {
  val data: java.util.Map<String,Int> = java.util.HashMap<String, Int>()

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

  fun testSetViaIndexOperators() {
    val map = java.util.HashMap<String, String>()
    assert{ map.empty }
    // TODO cannot use map.size due to compiler bug
    assertEquals(map.size(), 0)

    map["name"] = "James"

    assert{ !map.empty }
    // TODO cannot use map.size due to compiler bug
    assertEquals(map.size(), 1)
    assertEquals("James", map["name"])
  }
}
