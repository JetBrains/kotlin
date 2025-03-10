// KIND: STANDALONE
// MODULE: StdlibUsages
// FILE: main.kt

fun produceByteArray(): ByteArray {
    return byteArrayOf(1, 2, 3)
}

fun ByteArray.getElementAt(index: Int) =
    this[index]