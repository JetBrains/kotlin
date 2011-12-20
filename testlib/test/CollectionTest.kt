namespace test.collections

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
      data.all{s => s.startsWith("b")}
    }
  }

  fun testFilter() {
    val foo = data.filter{it.startsWith("f")}

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
    /**
      TODO compiler bug
      we should be able to remove the explicit type on the function
      http://youtrack.jetbrains.net/issue/KT-849
    */
    // TODO there should be a neater way to do this :)

    val characters = arrayList('f', 'o', 'o', 'b', 'a', 'r')
    /*
    val characters = data.flatMap<String,Character>{
      Arrays.asList((it as java.lang.String).toCharArray()) as Collection<Character>
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
    val x = data.foreach{ count += it.length }
    assertEquals(6, count)
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
    val lengths = data.map<String,Int>{s => s.length}
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