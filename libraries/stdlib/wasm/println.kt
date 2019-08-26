/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.internal.WasmImport
import kotlin.wasm.internal.implementedAsIntrinsic

@WasmImport("runtime", "println")
fun println(x: String): Unit =
    implementedAsIntrinsic