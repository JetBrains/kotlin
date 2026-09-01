// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inspectPrimitiveArray.in
// OUTPUT_DATA_FILE: inspectPrimitiveArray.out



fun main(args: Array<String>) {
    val intArray = intArrayOf(1, 2, 3)
    val emptyIntArray = intArrayOf()
    val longArray = longArrayOf(10L, 20L, 30L)
    val shortArray = shortArrayOf(4, 5, 6)
    val byteArray = byteArrayOf(-1, 0, 1)
    val charArray = charArrayOf('A', 'B', 'C')
    val booleanArray = booleanArrayOf(true, false, true)
    val floatArray = floatArrayOf(1.5f, 2.5f, 3.5f)
    val doubleArray = doubleArrayOf(10.25, 20.5, 30.75)
    return
}
