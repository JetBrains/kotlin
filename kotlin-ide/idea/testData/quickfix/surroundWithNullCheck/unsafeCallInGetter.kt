// "Surround with null check" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Convert property getter to initializer
// ACTION: Convert to block body
// ACTION: Introduce local variable
// ACTION: Replace with safe (?.) call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?

class My(val x: String?) {
    val y: Int
        get() = x<caret>.hashCode()
}