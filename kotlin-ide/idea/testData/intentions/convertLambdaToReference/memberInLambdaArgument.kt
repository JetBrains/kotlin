// IS_APPLICABLE: true
// WITH_RUNTIME

class Owner(val z: Int) {
    fun foo(y: Int) = y + z
    val x = 7.let {<caret> foo(it) }
}