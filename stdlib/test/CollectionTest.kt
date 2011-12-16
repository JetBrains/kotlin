namespace test.collections

import std.test.*

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import std.io.*
import std.util.*
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

}