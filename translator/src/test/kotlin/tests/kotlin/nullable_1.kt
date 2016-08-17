class nullable_1_MyAwesomeClass(var i: Int)

fun nullable_1_test(i: Int): Int {
    var x: nullable_1_MyAwesomeClass? = null
    x = nullable_1_MyAwesomeClass(i)
    return x.i
}

fun nullable_1_test_2(i: Int): Int {
    var x: nullable_1_MyAwesomeClass? = nullable_1_MyAwesomeClass(i)
    x = null
    x = nullable_1_MyAwesomeClass(i)

    return x.i + 1
}

fun nullable_1_test_2_npe_operator(i: Int): Int {
    val x: nullable_1_MyAwesomeClass? = nullable_1_MyAwesomeClass(i)
    x!!
    return x.i + 555
}