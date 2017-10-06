package html5.minimal

// This file will be auto-generated.

import kotlinx.wasm.jsinterop.*

@SymbolName("knjs_addDocumentToArena")
external public fun addDocumentToArena(arena: Arena): Int

@SymbolName("knjs_getElementById")
external public fun knjs_getElementById(arena: Int, obj: Int, idPtr: Int, idLen: Int, resultArena: Int): Int;

@SymbolName("knjs_getContext")
external public fun knjs_getContext(arena: Int, obj: Int, idPtr: Int, idLen: Int, resultArena: Int): Int;

@SymbolName("knjs_getBoundingClientRect")
external public fun knjs_getBoundingClientRect(arena: Int, obj: Int, resultArena: Int): Int;

@SymbolName("knjs_beginPath")
external public fun knjs_beginPath(arena: Int, obj: Int);

@SymbolName("knjs_stroke")
external public fun knjs_stroke(arena: Int, obj: Int);

@SymbolName("knjs_moveTo")
external public fun knjs_moveTo(arena: Int, obj: Int, x: Int, y: Int): Int;

@SymbolName("knjs_lineTo")
external public fun knjs_lineTo(arena: Int, obj: Int, x: Int, y: Int): Int;

@SymbolName("knjs_fillRect")
external public fun knjs_fillRect(arena: Int, obj: Int, x1: Int, y1: Int, width: Int, height: Int)
@SymbolName("knjs_fillText")
external public fun knjs_fillText(arena: Int, obj: Int, textPtr: Int, textWidth: Int, x: Int, y: Int, maxWidth: Int)

@SymbolName("knjs_fill")
external public fun knjs_fill(arena: Int, obj: Int)

@SymbolName("knjs_closePath")
external public fun knjs_closePath(arena: Int, obj: Int)

@SymbolName("knjs_setLineWidth")
external public fun knjs_setLineWidth(thisArena: Int, thisIndex: Int, value: Int)
@SymbolName("knjs_setFillStyle")
external public fun knjs_setFillStyle(thisArena: Int, thisIndex: Int, valuePtr: Int, valueLength: Int)

@SymbolName("knjs_setInterval")
external public fun knjs_setInterval(arenaIndex: Int, functionIndex: Int, interval: Int);

@SymbolName("knjs_fetch")
external public fun knjs_fetch(arena: Int, urlPtr: Int, urlLen: Int, resultArena: Int): Int;

@SymbolName("knjs_then")
external public fun knjs_then(thisArena: Int, thisIndex: Int, functionIndex: Int, resultArena: Int): Int

@SymbolName("knjs_json")
external public fun knjs_json(thisArena: Int, thisIndex: Int, resultArena: Int): Int

open class Document(arena: Int, index: Int): JsValue(arena, index) {
    constructor(value: JsValue): this(value.arena, value.index)
    fun getElementById(id: String): JsValue {
        val resultIndex = knjs_getElementById(this.arena, this.index, stringPointer(id), stringLengthBytes(id), ArenaManager.currentArena)
        return JsValue(ArenaManager.currentArena, resultIndex)
    }
}

open class DOMRect(arena: Int, index: Int): JsValue(arena, index) {
}

open class Canvas(arena: Int, index: Int): JsValue(arena, index) {
    fun getContext(context: String): Context {
        val resultIndex = knjs_getContext(this.arena, this.index, stringPointer(context), stringLengthBytes(context), ArenaManager.currentArena)
        return Context(ArenaManager.currentArena, resultIndex)
    }
    fun getBoundingClientRect(): DOMRect {
        return DOMRect(ArenaManager.currentArena, knjs_getBoundingClientRect(this.arena, this.index, ArenaManager.currentArena))
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
    fun beginPath() {
        knjs_beginPath(this.arena, this.index);
    }
    fun stroke() {
        knjs_stroke(this.arena, this.index);
    }
    fun fillRect(x: Int, y: Int, width: Int, height: Int) {
        knjs_fillRect(this.arena, this.index, x, y, width, height)
    }
    fun fillText(text: String, x: Int, y: Int, maxWidth: Int) {
        knjs_fillText(this.arena, this.index, stringPointer(text), stringLengthBytes(text), x, y, maxWidth)
    }
    fun fill() {
        knjs_fill(this.arena, this.index)
    }
    fun closePath() {
        knjs_closePath(this.arena, this.index)
    }
    var lineWidth: Int
        get() = TODO("implement me")
        set(value: Int) {
            knjs_setLineWidth(this.arena, this.index, value)
        }
    var fillStyle: String
        get() = TODO("implement me")
        set(value: String) {
            knjs_setFillStyle(this.arena, this.index, stringPointer(value), stringLengthBytes(value))
        }
}


class Response(arena: Int, index: Int): JsValue(arena, index) {
    constructor(value: JsValue): this(value.arena, value.index)
    fun json(): JsValue {
        return JsValue(ArenaManager.currentArena, knjs_json(this.arena, this.index, ArenaManager.currentArena))
    }
}

class Html5() { 
    val document = Document(ArenaManager.currentArena, addDocumentToArena(ArenaManager.currentArena))

    fun fetch(url: String): Promise {
        return Promise (ArenaManager.currentArena, knjs_fetch(ArenaManager.currentArena, stringPointer(url), stringLengthBytes(url), ArenaManager.currentArena))
    }

    fun setInterval(lambda: KtFunction<Unit>, interval: Int) {
        knjs_setInterval(ArenaManager.globalArena, wrapFunction(lambda), interval);
    }

    // TODO: The arguments are flipped to have lambda within the last position.
    fun setInterval(interval: Int, lambda: KtFunction<Unit>) 
        = setInterval(lambda, interval)

}

open class Promise (arena: Int, index: Int): JsValue(arena, index) {
    fun <R> then(lambda: KtFunction<R>): Promise {
        return Promise(ArenaManager.currentArena, knjs_then(arena, index, wrapFunction<R>(lambda), ArenaManager.currentArena))
    }
}


