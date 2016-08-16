fun array_extensions_1_copyOf(): Int {
    val array = IntArray(3)
    array[0] = 1
    array[1] = 20
    array[2] = 333
    val minimize = array.copyOf(2)
    return minimize[1] + minimize.size
}

fun array_extensions_1_copyOf_extend(): Int {
    val array = IntArray(3)
    array[0] = 1
    array[1] = 20
    array[2] = 333
    val minimize = array.copyOf(7)
    return minimize[2] + minimize[3] + minimize[4] + minimize[5] + minimize[6] + minimize.size
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

fun array_extensions_1_plus_element(): Int {
    val array = IntArray(7)
    array[0] = 1
    array[1] = 20
    array[2] = 333
    array[3] = 444
    array[4] = 555
    array[5] = 666
    array[6] = 777

    val minimize = array.plus(2148)
    return minimize[6] + minimize[7] + minimize.size
}

fun array_extensions_1_plus_array(): Int {
    val array = IntArray(3)
    array[0] = 1
    array[1] = 20
    array[2] = 333
    val secondArray = IntArray(4)
    secondArray[0] = 9999
    secondArray[1] = 2789
    secondArray[2] = 11792
    secondArray[3] = 67820

    val maximize = array.plus(secondArray)
    val ans = maximize[2] + maximize[5] + maximize.size
    return ans
}

fun array_extensions_2_plus_array(): Int {
    var array = IntArray(0)
    val nextArray = IntArray(1)
    nextArray[0] = 10

    array = array.plus(nextArray)
    println(array[0])

    return array[0]
}
