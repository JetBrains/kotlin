// DISABLE_NATIVE: gcType=NOOP
import kotlin.native.runtime.GC
import kotlin.test.*

object Foo {
    val bar = Bar()
}

class Bar {
    val f by lazy {
        foo()
    }

    fun foo() = 123
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Test
fun test() {
    assertEquals(123, Foo.bar.f)
    GC.collect()
    assertEquals(123, Foo.bar.f)
}