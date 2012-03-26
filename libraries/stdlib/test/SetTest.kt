package test.collections

import kotlin.test.*
import java.util.*
import org.junit.Test

class SetTest {
    val data = hashSet("foo", "bar")

    Test fun any() {
        assertTrue {
            data.any{it.startsWith("f")}
        }
        assertNot {
            data.any{it.startsWith("x")}
        }
    }

    Test fun all() {
        assertTrue {
            data.all{it.length == 3}
        }
        assertNot {
            data.all{(s: String) -> s.startsWith("b")}
        }
    }

    Test fun filter() {
        val foo = data.filter{it.startsWith("f")}.toSet()

        assertTrue {
            foo.all{it.startsWith("f")}
        }
        assertEquals(1, foo.size)
        assertEquals(hashSet("foo"), foo)

        assertTrue("Filter on a Set should return a Set") {
            foo is Set<String>
        }
    }

    Test fun find() {
        val x = data.find{it.startsWith("x")}
        assertNull(x)
        fails {
            x.sure()
        }

        val f = data.find{it.startsWith("f")}
        f.sure()
        assertEquals("foo", f)
    }

    Test fun map() {
        /**
          TODO compiler bug
          we should be able to remove the explicit type on the function
          http://youtrack.jetbrains.net/issue/KT-849
        */
        val lengths = data.map<String, Int>{(s: String) -> s.length}
        assertTrue {
            lengths.all{it == 3}
        }
        assertEquals(2, lengths.size)
        assertEquals(arrayList(3, 3), lengths)
    }

}
