package foo

import jvmName.add
import org.junit.Assert
import org.junit.Test

class KTest {

    @Test
    fun testAdd() {
        Assert.assertEquals(6, add(3, 3))
    }
}