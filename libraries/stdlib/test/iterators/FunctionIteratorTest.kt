package iterators

import kotlin.*
import kotlin.test.*

import org.junit.Test

class FunctionIteratorTest {

    Test fun iterateOverFunction() {
        var count = 3

        val iter = iterate<Int> {
            count--
            if (count >= 0) count else null
        }

        val list = iter.toList()
        assertEquals(arrayList(2, 1, 0), list)
    }

    Test fun iterateOverFunction2() {
        val values = iterate<Int>(3) { n -> if (n > 0) n - 1 else null }
        assertEquals(arrayList(3, 2, 1, 0), values.toList())
    }
}