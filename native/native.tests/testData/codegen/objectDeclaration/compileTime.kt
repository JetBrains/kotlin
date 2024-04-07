import kotlin.test.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.native.internal.CanBePrecreated
object CompileTime {

    const val int = Int.MIN_VALUE
    const val byte = Byte.MIN_VALUE
    const val short = Short.MIN_VALUE
    const val long = Long.MIN_VALUE
    const val boolean = true
    const val float = 1.0f
    const val double = 1.0
    const val char = Char.MIN_VALUE
}

fun box(): String {
    assertEquals(Int.MIN_VALUE, CompileTime.int)
    assertEquals(Byte.MIN_VALUE, CompileTime.byte)
    assertEquals(Short.MIN_VALUE, CompileTime.short)
    assertEquals(Long.MIN_VALUE, CompileTime.long)
    assertEquals(true, CompileTime.boolean)
    assertEquals(1.0f, CompileTime.float)
    assertEquals(1.0, CompileTime.double)
    assertEquals(Char.MIN_VALUE, CompileTime.char)
    return "OK"
}