package test.collections

import stdhack.test.*

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import std.*
import std.io.*
import std.util.*
import java.util.*

class ListTest() : TestSupport() {
  val data = arrayList("foo", "bar")

  fun testHeadAndTail() {
    val h = data.head
    assertEquals("foo", h)

    val t = data.tail
    assertEquals("bar", t)
  }

  fun testFirstAndLast() {
    val h = data.first
    assertEquals("foo", h)

    val t = data.last
    assertEquals("bar", t)
  }
}
