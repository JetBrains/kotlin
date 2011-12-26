package test.collections

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import std.*
import std.io.*
import std.util.*
import std.test.*
import java.util.*

class CollectionTest() : TestSupport() {
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
    // TODO size should implement size property to be polymorphic with collections
    assertEquals(2, data.count{it.length == 3})
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
      http://youtrack.jetbrains.net/issue/KT-849
    */
    val lengths = data.map<String,Int>{s -> s.length}
    assert {
      lengths.all{it == 3}
    }
    assertEquals(2, lengths.size)
    assertEquals(arrayList(3, 3), lengths)
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
}