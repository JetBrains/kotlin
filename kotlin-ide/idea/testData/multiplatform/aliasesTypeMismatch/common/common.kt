@file:Suppress("UNUSED_PARAMETER")

expect open class MyCancelException : MyIllegalStateException

fun cancel(cause: MyCancelException) {}

expect open class OtherException : MyIllegalStateException

fun other(cause: OtherException) {}
