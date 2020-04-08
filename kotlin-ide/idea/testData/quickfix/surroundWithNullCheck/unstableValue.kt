// "Surround with null check" "false"
// ACTION: Introduce local variable
// ACTION: Add non-null asserted (!!) call
// ACTION: Replace with safe (?.) call
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Int?

class My(var x: Int?) {

    fun foo() {
        x<caret>.hashCode()
    }
}