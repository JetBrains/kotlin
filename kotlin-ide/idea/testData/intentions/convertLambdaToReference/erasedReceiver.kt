// IS_APPLICABLE: false

class Generic<T : Any> {
    val y = { arg: T <caret>-> arg.hashCode() }
}