package sample

expect class <!LINE_MARKER("descr='Has actuals in JS, JVM'")!>Sample<!>() {
    fun <!LINE_MARKER("descr='Has actuals in JS, JVM'")!>checkMe<!>(): Int
}

expect object <!LINE_MARKER("descr='Has actuals in JS, JVM'")!>Platform<!> {
    val <!LINE_MARKER("descr='Has actuals in JS, JVM'")!>name<!>: String
}

fun hello(): String = "Hello from ${Platform.name}"