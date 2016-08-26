class function_extensions_double_argument_1_slave(val buffer: IntArray) {
    var pos = 0

    fun read(): Int {
        pos += 1
        val pe = pos - 1
        return buffer[pe]
    }

    fun isAtEnd(): Boolean {
        return pos >= buffer.size
    }
}

fun function_extensions_double_argument_1_master(inputStream: function_extensions_double_argument_1_slave): Int {
    return inputStream.read().toInt()
}

fun function_extensions_double_argument_1(): Int {
    val array = IntArray(7)
    array[0] = 129
    array[1] = 20
    array[2] = 333
    array[3] = 444
    array[4] = 555
    array[5] = 666
    array[6] = 777

    val kis = function_extensions_double_argument_1_slave(array)
    return function_extensions_double_argument_1_master(kis)
}
