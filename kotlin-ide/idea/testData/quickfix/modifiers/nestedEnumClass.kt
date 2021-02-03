// "Add 'inner' modifier" "false"
// ACTION: Convert to sealed class
// ACTION: Create test
// ERROR: Enum class is not allowed here
class A() {
    inner class B() {
        enum class <caret>C
    }
}
/* FIR_COMPARISON */