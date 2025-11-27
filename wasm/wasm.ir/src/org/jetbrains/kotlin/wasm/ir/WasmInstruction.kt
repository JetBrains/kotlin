/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

sealed class WasmInstr(val operator: WasmOp) {
    abstract fun forEachImmediates(body: (WasmImmediate) -> Unit)
    abstract val immediatesCount: Int
    abstract val location: SourceLocation?
}

private open class WasmInstr0(
    operator: WasmOp,
) : WasmInstr(operator) {
    override val location: SourceLocation? get() = null
    override fun forEachImmediates(body: (WasmImmediate) -> Unit): Unit = Unit
    override val immediatesCount: Int get() = 0
}

private open class WasmInstr1(
    operator: WasmOp,
    val immediate1: WasmImmediate,
) : WasmInstr0(operator) {
    override fun forEachImmediates(body: (WasmImmediate) -> Unit): Unit = body(immediate1)
    override val immediatesCount: Int get() = 1
}

private open class WasmInstr2(
    operator: WasmOp,
    immediate1: WasmImmediate,
    val immediate2: WasmImmediate,
) : WasmInstr1(operator, immediate1) {
    override fun forEachImmediates(body: (WasmImmediate) -> Unit) {
        body(immediate1); body(immediate2)
    }

    override val immediatesCount: Int get() = 2
}

private open class WasmInstr3(
    operator: WasmOp,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    val immediate3: WasmImmediate,
) : WasmInstr2(operator, immediate1, immediate2) {
    override fun forEachImmediates(body: (WasmImmediate) -> Unit) {
        body(immediate1); body(immediate2); body(immediate3)
    }

    override val immediatesCount: Int get() = 3
}

private open class WasmInstr4(
    operator: WasmOp,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
    val immediate4: WasmImmediate,
) : WasmInstr3(operator, immediate1, immediate2, immediate3) {
    override fun forEachImmediates(body: (WasmImmediate) -> Unit) {
        body(immediate1); body(immediate2); body(immediate3); body(immediate4)
    }

    override val immediatesCount: Int get() = 4
}

private open class WasmInstr0Located(
    operator: WasmOp,
    override val location: SourceLocation,
) : WasmInstr0(operator)

private open class WasmInstr1Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
) : WasmInstr1(operator, immediate1)

private open class WasmInstr2Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
) : WasmInstr2(operator, immediate1, immediate2)

private open class WasmInstr3Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
) : WasmInstr3(operator, immediate1, immediate2, immediate3)

private open class WasmInstr4Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
    immediate4: WasmImmediate,
) : WasmInstr4(operator, immediate1, immediate2, immediate3, immediate4)

private val wasmInst0cache = mutableMapOf<WasmOp, WasmInstr0>()
fun wasmInstrWithoutLocation(operator: WasmOp): WasmInstr =
    wasmInst0cache.getOrPut(operator) { WasmInstr0(operator) }

fun wasmInstrWithLocation(
    operator: WasmOp,
    location: SourceLocation,
): WasmInstr =
    if (location == SourceLocation.NoLocation) wasmInstrWithoutLocation(operator)
    else WasmInstr0Located(operator, location)

fun wasmInstrWithoutLocation(
    operator: WasmOp,
    immediate1: WasmImmediate,
): WasmInstr =
    WasmInstr1(operator, immediate1)

fun wasmInstrWithLocation(
    operator: WasmOp,
    location: SourceLocation,
    immediate1: WasmImmediate,
): WasmInstr =
    if (location == SourceLocation.NoLocation) wasmInstrWithoutLocation(operator, immediate1)
    else WasmInstr1Located(operator, location, immediate1)

fun wasmInstrWithoutLocation(
    operator: WasmOp,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
): WasmInstr =
    WasmInstr2(operator, immediate1, immediate2)

fun wasmInstrWithLocation(
    operator: WasmOp,
    location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
): WasmInstr =
    if (location == SourceLocation.NoLocation) wasmInstrWithoutLocation(operator, immediate1, immediate2)
    else WasmInstr2Located(operator, location, immediate1, immediate2)

fun wasmInstrWithoutLocation(
    operator: WasmOp,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
): WasmInstr =
    WasmInstr3(operator, immediate1, immediate2, immediate3)

fun wasmInstrWithLocation(
    operator: WasmOp,
    location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
): WasmInstr =
    if (location == SourceLocation.NoLocation) wasmInstrWithoutLocation(operator, immediate1, immediate2, immediate3)
    else WasmInstr3Located(operator, location, immediate1, immediate2, immediate3)

fun wasmInstrWithoutLocation(
    operator: WasmOp,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
    immediate4: WasmImmediate,
): WasmInstr =
    WasmInstr4(operator, immediate1, immediate2, immediate3, immediate4)

fun wasmInstrWithLocation(
    operator: WasmOp,
    location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
    immediate4: WasmImmediate,
): WasmInstr =
    if (location == SourceLocation.NoLocation) wasmInstrWithoutLocation(operator, immediate1, immediate2, immediate3, immediate4)
    else WasmInstr4Located(operator, location, immediate1, immediate2, immediate3, immediate4)

fun WasmInstr.firstImmediateOrNull(): WasmImmediate? =
    (this as? WasmInstr1)?.immediate1

fun WasmInstr.secondImmediateOrNull(): WasmImmediate? =
    (this as? WasmInstr2)?.immediate2

fun WasmInstr.thirdImmediateOrNull(): WasmImmediate? =
    (this as? WasmInstr3)?.immediate3

fun WasmInstr.fourthImmediateOrNull(): WasmImmediate? =
    (this as? WasmInstr4)?.immediate4