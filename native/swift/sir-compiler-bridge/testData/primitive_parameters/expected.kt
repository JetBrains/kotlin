import kotlin.native.internal.ExportForCppRuntime

@ExportForCppRuntime("a")
public fun a(p0: Byte, p1: Short, p2: Int, p3: Long): Int {
    val result = pkg.a(p0, p1, p2, p3)
    return result
}

@ExportForCppRuntime("b")
public fun b(p0: UByte, p1: UShort, p2: UInt, p3: ULong): UInt {
    val result = pkg.b(p0, p1, p2, p3)
    return result
}

@ExportForCppRuntime("c")
public fun c(p0: Boolean): Boolean {
    val result = pkg.c(p0)
    return result
}

