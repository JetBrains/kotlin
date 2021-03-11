package sample

actual class <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> /* Left */ {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>foo<!>(): Int = 42
    fun fromLeft(): String = ""
}
