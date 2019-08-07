/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.wasm_unreachable

fun assertEquals(x: Boolean, y: Boolean) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Byte, y: Byte) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Short, y: Short) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Char, y: Char) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Int, y: Int) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Long, y: Long) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Float, y: Float) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Double, y: Double) {
    if (x != y) wasm_unreachable()
}


fun assertEquals(x: Boolean, y: Boolean, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Byte, y: Byte, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Short, y: Short, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Char, y: Char, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Int, y: Int, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Long, y: Long, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Float, y: Float, s: String) {
    if (x != y) wasm_unreachable()
}
fun assertEquals(x: Double, y: Double, s: String) {
    if (x != y) wasm_unreachable()
}