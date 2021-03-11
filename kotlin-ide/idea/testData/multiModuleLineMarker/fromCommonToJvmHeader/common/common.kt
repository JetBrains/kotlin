expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>Header<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>foo<!>(): Int
}

expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>Incomplete<!> {
    fun foo(): Int
}

expect fun <!LINE_MARKER("descr='Has actuals in JVM'")!>foo<!>(arg: Int): String

expect val <!LINE_MARKER("descr='Has actuals in JVM'")!>flag<!>: Boolean
