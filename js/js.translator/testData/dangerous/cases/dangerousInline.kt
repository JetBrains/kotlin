package foo

var i = 0

inline fun f() = i * 2

fun box(): Boolean {
    return (++i + f()) == 3
}