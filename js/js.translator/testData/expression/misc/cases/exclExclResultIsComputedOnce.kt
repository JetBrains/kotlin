package foo

var c = 0
val a: Int?
    get() {
        c++
        return 2
    }

fun box(): Boolean {
    return c == 0 && (a!! + 3) == 5 && c == 1
}