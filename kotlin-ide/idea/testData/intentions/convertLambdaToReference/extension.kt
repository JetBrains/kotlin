// IS_APPLICABLE: true

class Owner(val z: Int) {
    val x = { arg: Int <caret> -> foo(arg) }
}

fun Owner.foo(y: Int) = y + z

