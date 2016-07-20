
class MyClass(val b: Int, var c: Int)

fun genMyClass(i: Int): MyClass {
    return MyClass(i, i)
}

fun change(x: Int): Int {

    val y = MyClass(x, x)
    y.c = x
    y.c = x
    y.c = 1
    y.c = x + 1

    return y.c
}

fun testGen(i: Int): Int {
    val j = genMyClass(i)
    return j.b
}
