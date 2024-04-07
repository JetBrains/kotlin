import kotlin.native.internal.ExportedBridge

@ExportedBridge("a__TypesOfArguments__int8_t_int16_t_int32_t_int64_t__")
public fun a(p0: Byte, p1: Short, p2: Int, p3: Long): Int {
    val __p0 = p0
    val __p1 = p1
    val __p2 = p2
    val __p3 = p3
    val _result = pkg.a(__p0, __p1, __p2, __p3)
    return _result
}

@ExportedBridge("b__TypesOfArguments__uint8_t_uint16_t_uint32_t_uint64_t__")
public fun b(p0: UByte, p1: UShort, p2: UInt, p3: ULong): UInt {
    val __p0 = p0
    val __p1 = p1
    val __p2 = p2
    val __p3 = p3
    val _result = pkg.b(__p0, __p1, __p2, __p3)
    return _result
}

@ExportedBridge("c__TypesOfArguments___Bool__")
public fun c(p0: Boolean): Boolean {
    val __p0 = p0
    val _result = pkg.c(__p0)
    return _result
}

