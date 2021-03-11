// IS_APPLICABLE: true

class Owner(val z: Int) {
    fun foo(y: Int) = y + z
    val x = { arg: Int <caret> -> foo(arg) }
}