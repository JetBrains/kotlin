package foo

var a = 3

fun box(): Boolean {
    a -= 10

    return (a == -7)

}