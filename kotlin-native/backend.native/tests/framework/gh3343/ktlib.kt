import kotlinx.cinterop.*
import objclib.*
import kotlin.native.ref.*

fun run(): List<Any?> {
    val result = mutableListOf<Any?>()
    result.add(foo1(42))
    val list = foo2(117)
    if (list != null) {
        result.add(list.size)
        for (x in list)
            result.add(x)
    }
    return result
}