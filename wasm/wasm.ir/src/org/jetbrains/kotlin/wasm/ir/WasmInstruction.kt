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

open class WasmInstr0(
    operator: WasmOp,
) : WasmInstr(operator) {
    override val location: SourceLocation? get() = null
    override fun forEachImmediates(body: (WasmImmediate) -> Unit): Unit = Unit
    override val immediatesCount: Int get() = 0
}

open class WasmInstr1(
    operator: WasmOp,
    val immediate1: WasmImmediate,
) : WasmInstr0(operator) {
    override fun forEachImmediates(body: (WasmImmediate) -> Unit): Unit = body(immediate1)
    override val immediatesCount: Int get() = 1
}

open class WasmInstr2(
    operator: WasmOp,
    immediate1: WasmImmediate,
    val immediate2: WasmImmediate,
) : WasmInstr1(operator, immediate1) {
    override fun forEachImmediates(body: (WasmImmediate) -> Unit) {
        body(immediate1); body(immediate2)
    }

    override val immediatesCount: Int get() = 2
}

open class WasmInstr3(
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

open class WasmInstr4(
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

open class WasmInstr0Located(
    operator: WasmOp,
    override val location: SourceLocation,
) : WasmInstr0(operator)

open class WasmInstr1Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
) : WasmInstr1(operator, immediate1)

open class WasmInstr2Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
) : WasmInstr2(operator, immediate1, immediate2)

open class WasmInstr3Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
) : WasmInstr3(operator, immediate1, immediate2, immediate3)

open class WasmInstr4Located(
    operator: WasmOp,
    override val location: SourceLocation,
    immediate1: WasmImmediate,
    immediate2: WasmImmediate,
    immediate3: WasmImmediate,
    immediate4: WasmImmediate,
) : WasmInstr4(operator, immediate1, immediate2, immediate3, immediate4)