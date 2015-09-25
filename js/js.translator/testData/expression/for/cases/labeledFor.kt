package foo

val a1 = arrayOfNulls<Int>(0)

fun box(): Boolean {
    var bar = 33
    outer@ for (a in arrayOf(1, 2)) {
        for (b in arrayOf(1, 4)) {
            break@outer
        }
        bar = 42
    }
    return bar == 33;
}