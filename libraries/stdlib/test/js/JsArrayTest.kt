package test.collections

import org.junit.Test as test
import kotlin.test.*
import java.util.ArrayList;

class JsArrayTest {

    @test fun arraySizeAndToList() {
        val a1 = arrayOf<String>()
        val a2 = arrayOf("foo")
        val a3 = arrayOf("foo", "bar")

        assertEquals(0, a1.size)
        assertEquals(1, a2.size)
        assertEquals(2, a3.size)

        assertEquals("[]", a1.toList().toString())
        assertEquals("[foo]", a2.toList().toString())
        assertEquals("[foo, bar]", a3.toList().toString())

    }

    @test fun arrayListFromCollection() {
        var c: Collection<String>  = arrayOf("A", "B", "C").toList()
        var a = ArrayList(c)

        assertEquals(3, a.size)
        assertEquals("A", a[0])
        assertEquals("B", a[1])
        assertEquals("C", a[2])
    }
}
