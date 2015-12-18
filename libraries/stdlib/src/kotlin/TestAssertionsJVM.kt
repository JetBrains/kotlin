@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestAssertionsKt")
package kotlin.test

@Deprecated("For binary compatibility here.", level = DeprecationLevel.HIDDEN)
fun fail(message: String? = null) {
    throw AssertionError(message ?: "")
}
