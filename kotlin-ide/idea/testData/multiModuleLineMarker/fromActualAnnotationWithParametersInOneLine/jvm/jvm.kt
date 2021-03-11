actual annotation class <!LINE_MARKER("descr='Has declaration in common module'")!>Ann<!>(
        actual val <!LINE_MARKER("descr='Has declaration in common module'")!>x<!>: Int, actual val y: String,
        actual val <!LINE_MARKER("descr='Has declaration in common module'")!>z<!>: Double, actual val b: Boolean
)

/*
LINEMARKER: Has declaration in common module
TARGETS:
common.kt
expect annotation class <1>Ann(
*//*
LINEMARKER: Has declaration in common module
TARGETS:
common.kt
        val <1>x: Int, val <2>y: String,
*//*
LINEMARKER: Has declaration in common module
TARGETS:
common.kt
        val <2>z: Double, val <1>b: Boolean
*/

