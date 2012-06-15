package test.collections

import kotlin.test.*

import kotlin.io.*
import kotlin.util.*
import java.io.*
import java.util.*
import junit.framework.TestCase

class IoTest() : TestCase() {
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

    reader.use{
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
        reader.lines.forEach { ... }

      as we could one day maybe one day write that as
        for (line in reader.lines)

      if the for(elem in thing) {...} statement could act as syntax sugar for
        thing.forEach{ elem -> ... }

      if thing is not an Iterable/array/Iterator but has a suitable forEach method
    */
    reader.forEachLine{
      list.add(it)
    }

    assertEquals(arrayList("Hello", "World"), list)
  }
  
  fun testListFiles() {
    val dir = File.createTempFile("temp", System.nanoTime().toString())!!
    dir.delete()
    dir.mkdir()
    
    File.createTempFile("temp", "1.kt", dir)
    File.createTempFile("temp", "2.java", dir)
    File.createTempFile("temp", "3.kt", dir)
    
    val result = dir.listFiles { it.getName()!!.endsWith(".kt") }
    
    assertNotNull(result)
    assertEquals(result!!.size, 2)
  }
}
