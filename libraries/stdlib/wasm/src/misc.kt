/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.wasm_unreachable

fun THROW_ISE(): Nothing {
    wasm_unreachable()
}
fun THROW_CCE(): Nothing {
    wasm_unreachable()
}
fun THROW_NPE(): Nothing {
    wasm_unreachable()
}
fun THROW_IAE(msg: String): Nothing {
    wasm_unreachable()
}