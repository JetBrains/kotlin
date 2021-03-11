// "Make 'A' open" "false"
// ERROR: This type is final, so it cannot be inherited from
// ACTION: Add names to call arguments
// ACTION: Do not show hints for current method
// ACTION: Introduce import alias
data class A(val x: Int)
class B: A<caret>(42)