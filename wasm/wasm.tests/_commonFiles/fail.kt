package kotlin
// This file should be excluded from tests using StdLib, as these methods conflict with corresponding methods from kotlin.test
// see StdLibTestBase.removeAdHocAssertions

fun fail(message: String? = null): Nothing {
    throw Throwable(message)
}