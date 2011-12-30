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
    return InputStreamReader((this as java.lang.Object).getClass()?.getClassLoader()?.getResourceAsStream("test/HelloWorld.txt"))
  }

  fun testLineIterator() {
    /*
    // TODO compiler error
    // both these expressions causes java.lang.NoClassDefFoundError: collections/namespace
    val list = sample().useLines{it.toArrayList()}
    val list = sample().useLines<ArrayList<String>>{it.toArrayList()}

    assertEquals(arrayList("Hello", "World"), list)
    */
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
