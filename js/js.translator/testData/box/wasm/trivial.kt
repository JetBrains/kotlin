
fun foo(x: Int, y: Int): Int = x
fun foo2(x: Int, y: Int): Int = y

val x = 42

fun box(): Int {
    return x
//    if (true) {
//        return 42
//    } else {
//        return 0
//    }
}