namespace test.collections

import std.test.*

import std.io.*
import std.util.*
import java.io.*
import java.util.*

class IoTest() : TestSupport() {
  val file = File("test/HelloWorld.txt")

  fun testLineIterator() {
    val list = FileReader(file).buffered().lineIterator().toArrayList()
    assertEquals(arrayList("Hello", "World"), list)
  }

  fun testUse() {
    val list = ArrayList<String>()

    val reader = FileReader(file).buffered()
    reader.close()

    /**
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