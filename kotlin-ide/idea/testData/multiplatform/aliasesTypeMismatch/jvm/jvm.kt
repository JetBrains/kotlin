actual typealias <!LINE_MARKER("descr='Has declaration in common module'")!>MyCancelException<!> = platform.lib.MyCancellationException

actual open class <!LINE_MARKER("descr='Has declaration in common module'")!>OtherException<!> : platform.lib.MyIllegalStateException()

fun test() {
    cancel(MyCancelException()) // TYPE_MISMATCH

    other(OtherException())
}
