package test.collections

import kotlin.test.*

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import kotlin.*
import kotlin.io.*
import kotlin.util.*
import java.util.*
import junit.framework.TestCase

class ListTest() : TestCase() {
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

  fun testWithIndices() {
    val withIndices = data.withIndices()
    var index = 0
    for (withIndex in withIndices) {
      assertEquals(withIndex._1, index)
      assertEquals(withIndex._2, data[index])
      index++
    }
    assertEquals(data.size(), index)
  }
}
