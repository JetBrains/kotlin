package test.collections

import kotlin.test.*

import org.junit.Test

class ListTest {
  val data = arrayList("foo", "bar")

  Test fun headAndTail() {
    val h = data.head
    assertEquals("foo", h)

    val t = data.tail
    assertEquals("bar", t)
  }

  Test fun firstAndLast() {
    val h = data.first
    assertEquals("foo", h)

    val t = data.last
    assertEquals("bar", t)
  }

  Test fun withIndices() {
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
