package iterators

import kotlin.*
import kotlin.test.*
import kotlin.util.*

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
}