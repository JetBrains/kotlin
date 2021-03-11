class Owner(val z: Int) {
    fun foo(y: Int) = y + z
    val x = { y: Int -><caret> this.foo(y) }
}