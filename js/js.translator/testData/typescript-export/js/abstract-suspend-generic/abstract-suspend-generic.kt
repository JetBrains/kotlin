// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-90587
// MODULE: JS_TESTS
// FILE: abstract-suspend-generic.kt

package foo

@JsExport
abstract class RemoteValue<T : Any> internal constructor() {
    abstract suspend fun get(): T
}

private class IntRemoteValue : RemoteValue<Int>() {
    override suspend fun get(): Int = 42
}

@JsExport
fun createRemoteValue(): RemoteValue<Int> = IntRemoteValue()

@JsExport
suspend fun readRemoteValue(value: RemoteValue<Int>): Int = value.get()
