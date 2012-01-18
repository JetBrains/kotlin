package test.collections

import stdhack.test.*

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
    return StringReader("Hello\nWorld");
  }

  fun testLineIterator() {
    // TODO we should maybe zap the useLines approach as it encourages
    // use of iterators which don't close the underlying stream
    val list1 = sample().useLines{it.toArrayList()}
    val list2 = sample().useLines<ArrayList<String>>{it.toArrayList()}

    assertEquals(arrayList("Hello", "World"), list1)
    assertEquals(arrayList("Hello", "World"), list2)
  }

  fun testForEach() {
    val list = ArrayList<String>()
    val reader = sample().buffered()

    reader.foreach{
      while (true) {
        val line = it.readLine()
        if (line != null)
          list.add(line)
        else
          break
      }
    }

    assertEquals(arrayList("Hello", "World"), list)
  }

  fun testForEachLine() {
    val list = ArrayList<String>()
    val reader = sample()

    /* TODO would be nicer maybe to write this as
        reader.lines.foreach { ... }

      as we could one day maybe one day write that as
        for (line in reader.lines)

      if the for(elem in thing) {...} statement could act as syntax sugar for
        thing.foreach{ elem -> ... }

      if thing is not an Iterable/array/Iterator but has a suitable foreach method
    */
    reader.foreachLine{
      list.add(it)
    }

    assertEquals(arrayList("Hello", "World"), list)
  }
}
