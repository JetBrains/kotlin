package foo

data class OneThing(val x: String)

data class ThreeThings(val x: String, val y: Int, val z: OneThing, val t: Array<OneThing?>)

fun box(): String {
    val h = ThreeThings("hello world", 42, OneThing("foo"),
                        array(OneThing("hello"),
                              OneThing("world"),
                              null)).hashCode()
    if (h != 1667209151) {
        return "fail"
    }
    return "OK"
}
