package test.collections

import std.test.*

import std.io.*
import std.util.*
import java.io.*
import java.util.*

class IoTest() : TestSupport() {
  fun testLineIteratorWithManualClose() {
    val reader = sample().buffered()
    try {
      val list = reader.lineIterator().toArrayList()
      assertEquals(arrayList("Hello", "World"), list)
    } finally {
      reader.close()
    }
  }
  
  fun sample() : Reader {
    return StringReader("""Hello
World""");
  }

  fun testLineIterator() {
    val list1 = sample().useLines{it.toArrayList()}
    val list2 = sample().useLines<ArrayList<String>>{it.toArrayList()}

    assertEquals(arrayList("Hello", "World"), list1)
    assertEquals(arrayList("Hello", "World"), list2)
  }

  fun testUse() {
    /**
    val list = ArrayList<String>()
    val reader = sample().buffered()

    TODO compiler error?
    reader.use{
      val line = it.readLine()
      if (line != null) {
        list.add(line)
      }
    }

    assertEquals(arrayList("Hello", "World"), list)
    */
  }
}
