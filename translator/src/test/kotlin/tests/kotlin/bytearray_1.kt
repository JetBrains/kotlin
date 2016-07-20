fun bytearray_1(x: Byte): Byte {
    val z = ByteArray(10)
    z.set(1, x)
    return z.get(1)
}