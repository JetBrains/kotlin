package kotlin
// This file should be excluded from tests using StdLib, as these methods conflict with corresponding methods from kotlin.test
// see StdLibTestBase.removeAdHocAssertions

fun fail(message: String? = null): Nothing {
    // TODO: replace with next version when exception is supported properly
    // throw Exception(message)

    js("throw new Error(message)")
    null!!
}