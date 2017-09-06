package kotlinx.wasm.jsinterop

import konan.internal.ExportForCppRuntime
import kotlinx.cinterop.*

typealias Arena = Int
typealias Object = Int
typealias Pointer = Int

@SymbolName("Konan_js_allocateArena")
external public fun allocateArena(): Arena

@SymbolName("Konan_js_freeArena")
external public fun freeArena(arena: Arena)

@SymbolName("Kotlin_String_utf16pointer")
external public fun stringPointer(message: String): Pointer

@SymbolName("Kotlin_String_utf16length")
external public fun stringLengthBytes(message: String): Int

typealias KtFunction = (ArrayList<JsValue>)->Unit

fun wrapFunction(func: KtFunction): Int {
    val ptr: Long = StableObjPtr.create(func).value.toLong() 
    return ptr.toInt() // TODO: LP64 unsafe.
}

@ExportForCppRuntime("Konan_js_runLambda")
fun runLambda(userData: Int, arena: Arena, arenaSize: Int) {
    val arguments = arrayListOf<JsValue>()
    for (i in 0..arenaSize-1) {
        arguments.add(JsValue(arena, i));
    }
    // TODO: LP64 unsafe: wasm32 passes Int, not Long.
    val func = StableObjPtr.fromValue(userData.toLong().toCPointer()!!).get() as KtFunction 
    func(arguments);

    
}

open class JsValue(val arena: Arena, val index: Object) {
    fun getInt(property: String): Int {
        return getInt(arena, index, stringPointer(property), stringLengthBytes(property))
    }
}

@SymbolName("Konan_js_getInt")
external public fun getInt(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int;

@SymbolName("Konan_js_setFunction")
external public fun setFunction(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int , function: Int)

fun setter(obj: JsValue, property: String, lambda: (arguments: ArrayList<JsValue>) -> Unit) {
    val index = wrapFunction(lambda);
    setFunction(obj.arena, obj.index, stringPointer(property), stringLengthBytes(property), index)
}

fun JsValue.setter(property: String, lambda: (ArrayList<JsValue>) -> Unit) {
    setter(this, property, lambda)
}
