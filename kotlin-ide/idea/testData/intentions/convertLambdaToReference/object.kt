// IS_APPLICABLE: true

object Object {
    fun foo() = 42
}

val x = { o: Object -> o.foo()<caret> }