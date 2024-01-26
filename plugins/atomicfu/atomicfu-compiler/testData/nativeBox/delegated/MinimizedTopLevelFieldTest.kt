import kotlinx.atomicfu.*
import kotlin.test.*

// declaration of the volatile backing field for topLevelVInt property in a file causes an NPE
private var topLevelVInt by atomic(56)

fun testTopLevelVolatile() {
    assertEquals(56, topLevelVInt)
    topLevelVInt = 55
    assertEquals(110, topLevelVInt * 2)
}

class MinimizedTopLevelFieldTest {
    // declaration of the volatile backing field for vInt property inside a class -- is ok
    private var vInt by atomic(77)
    
    fun testVInt() {
        assertEquals(77, vInt)
        vInt = 55
        assertEquals(110, vInt * 2)
    }
}

@Test
fun box() {
    testTopLevelVolatile()
    MinimizedTopLevelFieldTest().testVInt()
}
