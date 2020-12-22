@file:Suppress("UNUSED_PARAMETER")

expect open class <!LINE_MARKER("descr='Has actuals in JVM'")!>MyCancelException<!> : MyIllegalStateException

fun cancel(cause: MyCancelException) {}

expect open class <!LINE_MARKER("descr='Has actuals in JVM'")!>OtherException<!> : MyIllegalStateException

fun other(cause: OtherException) {}
