
inline fun foo(x: Int, y: Int): Int = x
fun foo2(x: Int, y: Int): Int = y

val x = 42


var y = 20.0
fun box(): Int {
    y = 10.0
    y = 20.0
    return foo(x, 10)

//    if (true) {
//        return 42
//    } else {
//        return 0
//    }
}