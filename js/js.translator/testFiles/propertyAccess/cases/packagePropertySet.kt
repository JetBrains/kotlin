package foo

var b = 3

fun box(): Boolean {
    b = 2
    return (b == 2)
}