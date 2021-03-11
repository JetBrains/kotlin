// IS_APPLICABLE: false
val foo: <caret>Int.() -> String = {
    toString() + hashCode()
}