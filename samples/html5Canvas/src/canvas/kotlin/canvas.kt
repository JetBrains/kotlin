package html5.minimal

// This file will be auto-generated.

import kotlinx.wasm.jsinterop.*

@SymbolName("knjs_addDocumentToArena")
external public fun addDocumentToArena(arena: Arena): Int

@SymbolName("knjs_getElementById")
external public fun knjs_getElementById(arena: Int, obj: Int, idPtr: Int, idLen: Int): Int;

@SymbolName("knjs_getContext")
external public fun knjs_getContext(arena: Int, obj: Int, idPtr: Int, idLen: Int): Int;

@SymbolName("knjs_getBoundingClientRect")
external public fun knjs_getBoundingClientRect(arena: Int, obj: Int): Int;

@SymbolName("knjs_stroke")
external public fun knjs_stroke(arena: Int, obj: Int);

@SymbolName("knjs_moveTo")
external public fun knjs_moveTo(arena: Int, obj: Int, x: Int, y: Int): Int;

@SymbolName("knjs_lineTo")
external public fun knjs_lineTo(arena: Int, obj: Int, x: Int, y: Int): Int;

@SymbolName("knjs_setInterval")
external public fun knjs_setInterval(functionIndex: Int, interval: Int);

open class Document(arena: Int, index: Int): JsValue(arena, index) {
    fun getElementById(id: String): JsValue {
        val resultIndex = knjs_getElementById(this.arena, this.index, stringPointer(id), stringLengthBytes(id))
        return JsValue(this.arena, resultIndex)
    }
}

open class DOMRect(arena: Int, index: Int): JsValue(arena, index) {
}

open class Canvas(arena: Int, index: Int): JsValue(arena, index) {
    fun getContext(context: String): Context {
        val resultIndex = knjs_getContext(this.arena, this.index, stringPointer(context), stringLengthBytes(context))
        return Context(this.arena, resultIndex)
    }
    fun getBoundingClientRect(): DOMRect {
        return DOMRect(this.arena, knjs_getBoundingClientRect(this.arena, this.index))
    }
}

// TODO: this is awful.
val JsValue.asCanvas: Canvas
    get() {
        return Canvas(this.arena, this.index)
    }

open class Context(arena: Int, index: Int): JsValue(arena, index) {
    fun lineTo(x: Int, y: Int) {
        knjs_lineTo(this.arena, this.index, x, y)
    }
    fun moveTo(x: Int, y: Int) {
        knjs_moveTo(this.arena, this.index, x, y)
    }
    fun stroke() {
        knjs_stroke(this.arena, this.index);
    }
}
fun setInterval(lambda: (ArrayList<JsValue>)->Unit, interval: Int) {
    knjs_setInterval(wrapFunction(lambda), interval);
}

// TODO: The arguments are flipped to have lambda within the last position.
fun setInterval(interval: Int, lambda: (ArrayList<JsValue>)->Unit) 
    = setInterval(lambda, interval)

class html5 {
    val arena = allocateArena()
    val document = Document(arena, addDocumentToArena(arena))
}

