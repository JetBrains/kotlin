fun smartcast_1_CastCheck(x: Int): Int {
    return x + 1
}

fun smartcast_1(): Int {
    var x: Int? = null
    x = 3
    val y = x
    val uio = smartcast_1_CastCheck(y)
    return uio
}