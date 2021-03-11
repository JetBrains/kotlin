package localFunctionWithSingleLineExpressionBody

fun main() {
    //Breakpoint!
    fun bar() = "OK"
    val x = 1
    bar()
}

// STEP_OVER: 1
// The behavior of the two backends is different because they stop at two different instructions.
//   - The JVM backend stops on the declaration of bar on main:5, so stepping over proceeds to main:6
//   - The IR backend stops on the _body_ of bar when called from main:7, so proceeds to main:7