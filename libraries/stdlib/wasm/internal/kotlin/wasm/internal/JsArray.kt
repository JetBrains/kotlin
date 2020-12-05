/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.wasm.internal

@ExcludedFromCodegen
@WasmForeign
internal class WasmExternRef

@WasmImport("runtime", "JsArray_new")
internal fun JsArray_new(size: Int): WasmExternRef =
    implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Byte(array: WasmExternRef, index: Int): Byte = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Byte(array: WasmExternRef, index: Int, value: Byte): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Char(array: WasmExternRef, index: Int): Char = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Char(array: WasmExternRef, index: Int, value: Char): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Short(array: WasmExternRef, index: Int): Short = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Short(array: WasmExternRef, index: Int, value: Short): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Int(array: WasmExternRef, index: Int): Int = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Int(array: WasmExternRef, index: Int, value: Int): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Long(array: WasmExternRef, index: Int): Long = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Long(array: WasmExternRef, index: Int, value: Long): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Float(array: WasmExternRef, index: Int): Float = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Float(array: WasmExternRef, index: Int, value: Float): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Double(array: WasmExternRef, index: Int): Double = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Double(array: WasmExternRef, index: Int, value: Double): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Boolean(array: WasmExternRef, index: Int): Boolean = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Boolean(array: WasmExternRef, index: Int, value: Boolean): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_WasmExternRef(array: WasmExternRef, index: Int): WasmExternRef = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_WasmExternRef(array: WasmExternRef, index: Int, value: WasmExternRef): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_getSize")
internal fun JsArray_getSize(array: WasmExternRef): Int =
    implementedAsIntrinsic

@JsFun("(x) => x")
internal fun Any?.toWasmExternRef(): WasmExternRef =
    implementedAsIntrinsic

@JsFun("(x) => x")
internal fun WasmExternRefToAny(ref: WasmExternRef): Any? =
    implementedAsIntrinsic

internal inline fun JsArray_fill_Byte(array: WasmExternRef, size: Int, init: (Int) -> Byte) {
    var i = 0
    while (i < size) {
        JsArray_set_Byte(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Char(array: WasmExternRef, size: Int, init: (Int) -> Char) {
    var i = 0
    while (i < size) {
        JsArray_set_Char(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Short(array: WasmExternRef, size: Int, init: (Int) -> Short) {
    var i = 0
    while (i < size) {
        JsArray_set_Short(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Int(array: WasmExternRef, size: Int, init: (Int) -> Int) {
    var i = 0
    while (i < size) {
        JsArray_set_Int(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Long(array: WasmExternRef, size: Int, init: (Int) -> Long) {
    var i = 0
    while (i < size) {
        JsArray_set_Long(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Float(array: WasmExternRef, size: Int, init: (Int) -> Float) {
    var i = 0
    while (i < size) {
        JsArray_set_Float(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Double(array: WasmExternRef, size: Int, init: (Int) -> Double) {
    var i = 0
    while (i < size) {
        JsArray_set_Double(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Boolean(array: WasmExternRef, size: Int, init: (Int) -> Boolean) {
    var i = 0
    while (i < size) {
        JsArray_set_Boolean(array, i, init(i))
        i++
    }
}

internal inline fun <T> JsArray_fill_T(array: WasmExternRef, size: Int, init: (Int) -> T) {
    var i = 0
    while (i < size) {
        JsArray_set_WasmExternRef(array, i, init(i).toWasmExternRef())
        i++
    }
}