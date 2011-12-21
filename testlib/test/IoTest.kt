namespace test.collections

import std.test.*

import std.io.*
import std.util.*
import java.io.*

class IoTest() : TestSupport() {
  val file = File("test/HelloWorld.txt")

  fun testUse() {
    val list = arrayList<String>()

    val reader = FileReader(file).buffered()
    reader.close()

    /**
    TODO compiler error?
    reader.use<BufferedReader,Unit>{
      val line = it.readLine()
      if (line != null) {
        list.add(line)
      }
    }

    assertEquals(arrayList("Hello", "World"), list)
    */
  }

}