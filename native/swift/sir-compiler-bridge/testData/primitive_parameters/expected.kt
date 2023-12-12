import kotlin.native.internal.ExportedBridge

@ExportedBridge("a")
public fun a(p0: Byte, p1: Short, p2: Int, p3: Long): Int {
    val result = pkg.a(p0, p1, p2, p3)
    return result
}

@ExportedBridge("b")
public fun b(p0: UByte, p1: UShort, p2: UInt, p3: ULong): UInt {
    val result = pkg.b(p0, p1, p2, p3)
    return result
}

@ExportedBridge("c")
public fun c(p0: Boolean): Boolean {
    val result = pkg.c(p0)
    return result
}

