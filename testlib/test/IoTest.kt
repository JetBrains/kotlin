package test.collections

import std.test.*

import std.io.*
import std.util.*
import java.io.*
import java.util.*

class IoTest() : TestSupport() {
  val file = File("test/HelloWorld.txt")

  fun testLineIteratorWithManualClose() {
    val reader = FileReader(file).buffered()
    try {
      val list = reader.lineIterator().toArrayList()
      assertEquals(arrayList("Hello", "World"), list)
    } finally {
      reader.close()
    }
  }


  fun testLineIterator() {
    /*
    // TODO compiler error
    // both these expressions causes java.lang.NoClassDefFoundError: collections/namespace
    val list = FileReader(file).useLines{it.toArrayList()}
    val list = FileReader(file).useLines<ArrayList<String>>{it.toArrayList()}

    assertEquals(arrayList("Hello", "World"), list)
    */
  }

  fun testUse() {
    /**
    val list = ArrayList<String>()
    val reader = FileReader(file).buffered()

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