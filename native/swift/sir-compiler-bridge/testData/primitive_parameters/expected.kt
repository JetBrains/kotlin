import kotlin.native.internal.ExportedBridge

@ExportedBridge("a__TypesOfArguments__int8_t_int16_t_int32_t_int64_t__")
public fun a(p0: Byte, p1: Short, p2: Int, p3: Long): Int {
    val result = pkg.a(p0, p1, p2, p3)
    return result
}

@ExportedBridge("b__TypesOfArguments__uint8_t_uint16_t_uint32_t_uint64_t__")
public fun b(p0: UByte, p1: UShort, p2: UInt, p3: ULong): UInt {
    val result = pkg.b(p0, p1, p2, p3)
    return result
}

@ExportedBridge("c__TypesOfArguments___Bool__")
public fun c(p0: Boolean): Boolean {
    val result = pkg.c(p0)
    return result
}

