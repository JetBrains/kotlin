
class MyClass(val b: Int, var c: Int)

fun change(x: Int): Int {

    val y = MyClass(x, x)
    y.c = x
    y.c = x
    y.c = 1
    y.c = x + 1

    return y.c
}
