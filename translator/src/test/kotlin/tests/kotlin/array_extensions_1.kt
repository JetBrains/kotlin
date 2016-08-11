fun IntArray.print() {
    var index = 0
    while (index < size) {
        println(get(index))
        index++
    }
    println(1111111)
}


fun array_extensions_1_copyOf(): Int {
    val array = IntArray(3)
    array[0] = 1
    array[1] = 20
    array[2] = 333
    val minimize = array.copyOf(2)
    return minimize[1] + minimize.size
}

fun array_extensions_1_copyOfRange(): Int {
    val array = IntArray(7)
    array[0] = 1
    array[1] = 20
    array[2] = 333
    array[3] = 444
    array[4] = 555
    array[5] = 666
    array[6] = 777

    val minimize = array.copyOfRange(2, 4)
    val ans = minimize[0] + minimize[1] + minimize.size
    return ans
}