package test.collections

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import std.*
import std.io.*
import std.util.*
import stdhack.test.*
import java.util.*

class CollectionTest() : TestSupport() {

  class IterableWrapper<T>(collection : java.lang.Iterable<T>) : java.lang.Iterable<T> {
    private val collection = collection

    override fun iterator(): java.util.Iterator<T> {
      return collection.iterator().sure()
    }
  }


  val data = arrayList("foo", "bar")

  fun testAny() {
    assert {
      data.any{it.startsWith("f")}
    }
    assertNot {
      data.any{it.startsWith("x")}
    }
  }

  fun testAll() {
    assert {
      data.all{it.length == 3}
    }
    assertNot {
      data.all{s -> s.startsWith("b")}
    }
  }

  fun testCount() {
    assertEquals(1, data.count{it.startsWith("b")})
    assertEquals(2, data.count{it.size == 3})
  }

  fun testFilter() {
    val foo = data.filter{it.startsWith("f")}

    assert {
      foo.all{it.startsWith("f")}
    }
    assertEquals(1, foo.size)
    assertEquals(arrayList("foo"), foo)
  }

  fun testFilterNot() {
    val foo = data.filterNot{it.startsWith("b")}

    assert {
      foo.all{it.startsWith("f")}
    }
    assertEquals(1, foo.size)
    assertEquals(arrayList("foo"), foo)
  }

  fun testFilterIntoLinkedList() {
    // TODO would be nice to avoid the <String>
    val foo = data.filter(linkedList<String>()){it.startsWith("f")}

    assert {
      foo.all{it.startsWith("f")}
    }
    assertEquals(1, foo.size)
    assertEquals(linkedList("foo"), foo)

    assert {
      foo is LinkedList<String>
    }
  }

  fun testFilterIntoSortedSet() {
    // TODO would be nice to avoid the <String>
    val foo = data.filter(hashSet<String>()){it.startsWith("f")}

    assert {
      foo.all{it.startsWith("f")}
    }
    assertEquals(1, foo.size)
    assertEquals(hashSet("foo"), foo)

    assert {
      foo is HashSet<String>
    }
  }

  fun testFind() {
    val x = data.find{it.startsWith("x")}
    assertNull(x)
    fails {
      x.sure()
    }

    val f = data.find{it.startsWith("f")}
    f.sure()
    assertEquals("foo", f)
  }

  fun testFlatMap() {
    val characters = arrayList('f', 'o', 'o', 'b', 'a', 'r')
    // TODO figure out how to get a line like this to compile :)
    /*
    val characters = data.flatMap<String,Character>{
      it.toCharArray().toList() as Collection<Character>
    }
    */
    todo {
      println("Got list of characters ${characters}")
      val text = characters.join("")
      assertEquals("foobar", text)
    }
  }

  fun testForeach() {
    var count = 0
    data.foreach{ count += it.length }
    assertEquals(6, count)
  }

  fun testFold() {
    expect(10) {
      val numbers = arrayList(1, 2, 3, 4)

      // TODO would be nice to be able to write this as this
      //numbers.fold(0){it + it2}
      numbers.fold(0){(it, it2) -> it + it2}
    }

    expect(0) {
      val numbers = arrayList<Int>()
      numbers.fold(0){(it, it2) -> it + it2}
    }

    expect("1234") {
      val numbers = arrayList(1, 2, 3, 4)

      // TODO would be nice to be able to write this as this
      // numbers.map{it.toString()}.fold(""){it + it2}
      numbers.map<Int,String>{it.toString()}.fold(""){(it, it2) -> it + it2}
    }
  }

  fun testFoldRight() {
    expect("4321") {
      val numbers = arrayList(1, 2, 3, 4)

      // TODO would be nice to be able to write this as this
      // numbers.map{it.toString()}.foldRight(""){it + it2}
      numbers.map<Int,String>{it.toString()}.foldRight(""){(it, it2) -> it + it2}
    }
  }


  fun testGroupBy() {
    val words = arrayList("a", "ab", "abc", "def", "abcd")
    /*
     TODO inference engine should not need this type info?
     */
    val byLength = words.groupBy<String,Int>{it.length}
    assertEquals(4, byLength.size())

    println("Grouped by length is: $byLength")
    /*
     TODO compiler bug...

    val l3 = byLength.getOrElse(3, {ArrayList<String>()})
    assertEquals(2, l3.size)
    */

  }

  fun testJoin() {
    val text = data.join("-", "<", ">")
    assertEquals("<foo-bar>", text)
  }

  fun testMap() {
    /**
      TODO compiler bug
      we should be able to remove the explicit type on the function
      http://youtrack.jetbrains.net/issue/KT-1145
    */
    val lengths = data.map<String,Int>{s -> s.length}
    assert {
      lengths.all{it == 3}
    }
    assertEquals(2, lengths.size)
    assertEquals(arrayList(3, 3), lengths)
  }

  fun testReverse() {
    val rev = data.reverse()
    assertEquals(arrayList("bar", "foo"), rev)
  }

  fun testSort() {
    val coll: List<String> = arrayList("foo", "bar", "abc")

    // TODO fixme
    // Some sort of in/out variance thing - or an issue with Java interop?
    //coll.sort()
    todo {
      assertEquals(3, coll.size)
      assertEquals(arrayList("abc", "bar", "foo"), coll)

    }
  }

  fun testToArray() {
    val arr = data.toArray()
    println("Got array ${arr}")
    todo {
      assert {
        arr is Array<String>
      }
    }
  }

  fun testSimpleCount() {
    assertEquals(2, data.count())
    assertEquals(3, hashSet(12, 14, 15).count())
    assertEquals(0, ArrayList<Double>().count())
  }

  fun testLast() {
    assertEquals("bar", data.last())
    assertEquals(25, arrayList(15, 19, 20, 25).last())
    // assertEquals(19, TreeSet(arrayList(90, 47, 19)).first())
    assertEquals('a', linkedList('a').last())
  }

  fun testLastException() {
    fails { arrayList<Int>().last() }
    fails { linkedList<String>().last() }
    fails { hashSet<Char>().last() }
  }

  fun testIndices() {
    val indices = data.indices
    assertEquals(0, indices.start)
    assertEquals(1, indices.end)
    assertEquals(2, indices.size)
    assertFalse(indices.isReversed)
  }

  fun testContains() {
    assertTrue(data.contains("foo"))
    assertTrue(data.contains("bar"))
    assertFalse(data.contains("some"))

    // TODO: Problems with generation
//    assertTrue(IterableWrapper(data).contains("bar"))
//    assertFalse(IterableWrapper(data).contains("some"))

    assertFalse(hashSet<Int>().contains(12))
    assertTrue(linkedList(15, 19, 20).contains(15))

//    assertTrue(IterableWrapper(hashSet(45, 14, 13)).contains(14))
//    assertFalse(IterableWrapper(linkedList<Int>()).contains(15))
  }
}
