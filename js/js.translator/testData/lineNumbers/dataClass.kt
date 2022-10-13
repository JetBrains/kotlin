data class A(
        val x: Int,
        val y: String
)

// LINES(JS):    1 2 3 * 1 2 2 1 3 3 1 1 1 2 3 1 1 1 2 3 1 1 1 2 3 1 1 1 1 1 2 3

// FIXME: componentN function body should point to the corresponding property.
// LINES(JS_IR): 1 1 2 2 3 3 2 2 2 3 3 3 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
