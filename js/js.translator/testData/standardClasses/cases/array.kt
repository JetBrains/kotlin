package foo

fun box(): Boolean {

    val a = arrayOfNulls<Int>(2)
    a.set(1, 2)
    return a.get(1) == 2
}

