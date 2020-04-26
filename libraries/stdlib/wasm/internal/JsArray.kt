/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.wasm.internal

@ExcludedFromCodegen
@WasmForeign
internal class WasmAnyRef

@WasmImport("runtime", "JsArray_new")
internal fun JsArray_new(size: Int): WasmAnyRef =
    implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Byte(array: WasmAnyRef, index: Int): Byte = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Byte(array: WasmAnyRef, index: Int, value: Byte): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Char(array: WasmAnyRef, index: Int): Char = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Char(array: WasmAnyRef, index: Int, value: Char): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Short(array: WasmAnyRef, index: Int): Short = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Short(array: WasmAnyRef, index: Int, value: Short): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Int(array: WasmAnyRef, index: Int): Int = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Int(array: WasmAnyRef, index: Int, value: Int): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Long(array: WasmAnyRef, index: Int): Long = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Long(array: WasmAnyRef, index: Int, value: Long): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Float(array: WasmAnyRef, index: Int): Float = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Float(array: WasmAnyRef, index: Int, value: Float): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Double(array: WasmAnyRef, index: Int): Double = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Double(array: WasmAnyRef, index: Int, value: Double): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_Boolean(array: WasmAnyRef, index: Int): Boolean = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_Boolean(array: WasmAnyRef, index: Int, value: Boolean): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_get")
internal fun JsArray_get_WasmAnyRef(array: WasmAnyRef, index: Int): WasmAnyRef = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_set")
internal fun JsArray_set_WasmAnyRef(array: WasmAnyRef, index: Int, value: WasmAnyRef): Unit = implementedAsIntrinsic

@WasmImport("runtime", "JsArray_getSize")
internal fun JsArray_getSize(array: WasmAnyRef): Int =
    implementedAsIntrinsic

@WasmReinterpret
internal fun Any?.toWasmAnyRef(): WasmAnyRef =
    implementedAsIntrinsic

internal inline fun JsArray_fill_Byte(array: WasmAnyRef, size: Int, init: (Int) -> Byte) {
    var i = 0
    while (i < size) {
        JsArray_set_Byte(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Char(array: WasmAnyRef, size: Int, init: (Int) -> Char) {
    var i = 0
    while (i < size) {
        JsArray_set_Char(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Short(array: WasmAnyRef, size: Int, init: (Int) -> Short) {
    var i = 0
    while (i < size) {
        JsArray_set_Short(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Int(array: WasmAnyRef, size: Int, init: (Int) -> Int) {
    var i = 0
    while (i < size) {
        JsArray_set_Int(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Long(array: WasmAnyRef, size: Int, init: (Int) -> Long) {
    var i = 0
    while (i < size) {
        JsArray_set_Long(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Float(array: WasmAnyRef, size: Int, init: (Int) -> Float) {
    var i = 0
    while (i < size) {
        JsArray_set_Float(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Double(array: WasmAnyRef, size: Int, init: (Int) -> Double) {
    var i = 0
    while (i < size) {
        JsArray_set_Double(array, i, init(i))
        i++
    }
}

internal inline fun JsArray_fill_Boolean(array: WasmAnyRef, size: Int, init: (Int) -> Boolean) {
    var i = 0
    while (i < size) {
        JsArray_set_Boolean(array, i, init(i))
        i++
    }
}

internal inline fun <T> JsArray_fill_T(array: WasmAnyRef, size: Int, init: (Int) -> T) {
    var i = 0
    while (i < size) {
        JsArray_set_WasmAnyRef(array, i, init(i).toWasmAnyRef())
        i++
    }
}