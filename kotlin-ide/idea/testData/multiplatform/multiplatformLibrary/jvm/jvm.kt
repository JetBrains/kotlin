package sample

actual class <!LINE_MARKER("descr='Has declaration in common module'")!>Sample<!> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>checkMe<!>() = 42
}

actual object <!LINE_MARKER("descr='Has declaration in common module'")!>Platform<!> {
    actual val <!LINE_MARKER("descr='Has declaration in common module'")!>name<!>: String = "JVM"
}