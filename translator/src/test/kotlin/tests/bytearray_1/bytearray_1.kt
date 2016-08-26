fun bytearray_1(x: Byte): Byte {
    val z = ByteArray(10)
    z.set(1, x)
    val r = z.clone()
    return r.get(1)
}

fun bytearray_1_array(): Int {
    val size = 256
    val z= ByteArray(size)
    var ind = 0
    while(ind < size){
        z[ind] = ind.toByte()
        ind +=1
    }
    val newInstance = z.clone()
    ind = 0
    while(ind < size){
        assert(newInstance[ind] == ind.toByte())
        ind += 1
    }
    return 1
}
