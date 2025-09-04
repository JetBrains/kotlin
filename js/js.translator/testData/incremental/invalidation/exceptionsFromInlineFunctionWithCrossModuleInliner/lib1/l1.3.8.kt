inline fun foo() : Int {
    val x: Any? = null
    return x!! as Int
}
