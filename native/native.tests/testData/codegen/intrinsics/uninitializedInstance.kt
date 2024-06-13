import kotlin.native.internal.*
import kotlin.test.*

class Data(val x: Int, val p: Any)

fun box(): String {
    val data = createUninitializedInstance<Data>()
    assertEquals(0, data.x)
    assertNull(data.p)
    val x = 123
    val p = Any()
    initInstance(data, Data(x, p))
    assertEquals(x, data.x)
    assertSame(p, data.p)
    return "OK"
}