data class A(
        val x: Int,
        val y: String
)

// LINES(ClassicFrontend JS_IR): 1 2 3 1 2 2 3 3 2 2 2 3 3 3 1 1 1 1 1 1 1 2 3 1 1 1 1 2 3 1 1 * 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
// LINES(FIR JS_IR):             1 2 3 1 2 2 3 3 2 2 2 3 3 3 1 1 1 1 1 1 1 1 1 1 1 * 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
