// No lines
fun f1() {}
val p1 = 1
fun f2() {}

// test
fun f3() = 1
val p2 = 1
fun f4() = 1

fun f4() {}
val p3: Int
    get() = 1
fun f5() = 1

class OneLine {
    fun f1() {}

    val p1 = 1

    fun f2() {}

    fun f3() = 1

    val p2 = 1

    fun f4() = 1

    fun f4() {}

    val p3: Int
        get() = 1

    fun f5() = 1
}

class TwoLines {
    fun f1() {}


    val p1 = 1


    fun f2() {}


    fun f3() = 1


    val p2 = 1


    fun f4() = 1


    fun f4() {}


    val p3: Int
        get() = 1


    fun f5() = 1
}

