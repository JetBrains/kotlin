// KT-91505
// DONT_TARGET_EXACT_BACKEND: WASM_JS
package foo

external class Event(val type: String)

external interface IEventTarget {
    @Suppress("NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE")
    fun dispatchEvent(event: Event): String = definedExternally
}

open external class EventTarget: IEventTarget

class MyTarget: EventTarget()

fun box(): String {
    val event = Event("OK")
    return MyTarget().dispatchEvent(event)
}
