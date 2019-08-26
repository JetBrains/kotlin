fun box(): String {
    val x: Boolean = true
    val y: Any = x

    if (y != true)
        return "Fail 1"

//    if (y !== true)
//        return "Fail 2"

    return "OK"
}