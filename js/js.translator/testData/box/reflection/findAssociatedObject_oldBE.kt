// EXPECTED_REACHABLE_NODES: 1321
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

@Associated1(Bar::class)
class Foo

object Bar

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {
    // This API is not implented in the old backend.
    if (Foo::class.findAssociatedObject<Associated1>() != null) return "fail 1"

    return "OK"
}