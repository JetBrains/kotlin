fun foo1(): Int <selection>{
    val a = 1
    var b = 2

    return a - b
}</selection>

fun foo2(): Int {
    val x = 1
    var y = 2

    return x - y
}

fun foo3(): Int {
    val x = 1
    val y = 2

    return x - y
}

fun foo4(): Int {
    val a = 1
    var b = 0

    return a - b
}

val a = 1
var b = 2

fun foo5(): Int {
    return a - b
}