// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// MODULE: JS_TESTS
// FILE: abstract-suspend.kt

package foo

@JsExport
abstract class Base {
    abstract suspend fun value(x: Int): String
}

private class NamedImplementation : Base() {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
fun anonymous(): Base = object : Base() {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
fun named(): Base = NamedImplementation()

@JsExport
suspend fun callBase(base: Base): String = base.value(42)

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

@JsExport
abstract class BaseWithInterface {
    abstract suspend fun value(x: Int): String
}

@JsExport
interface ValueProvider {
    suspend fun value(x: Int): String
}

@JsExport
class CombinedImplementation : BaseWithInterface(), ValueProvider {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
suspend fun callCombinedBase(value: BaseWithInterface, x: Int): String = value.value(x)

@JsExport
suspend fun callCombinedInterface(value: ValueProvider, x: Int): String = value.value(x)
