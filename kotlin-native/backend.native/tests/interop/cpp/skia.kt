import kotlinx.cinterop.*
import kotlin.test.*

import skia.*

fun main() {
    val f = Foo()
    val a = nativeHeap.alloc<Value>()
    a.data = 17
    val x = f.foo(a.ptr)
    val z = f.bar(x)
    println("${a?.data} ${z?.pointed?.data}")
}
