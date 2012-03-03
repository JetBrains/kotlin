
import java.util.Vector
import junit.framework.TestCase
import kotlin.test.assertEquals

class EnumerationIteratorTest() : TestCase() {
    fun testIteration () {
        val v = Vector<Int>()
        for(i in 1..5)
            v.add(i)

        var sum = 0
        for(k in v.elements())
            sum += k

        assertEquals(15, sum)
    }
}