/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm

// TODO(REVIEW): happy to have this extend from a different base class
internal class WasiError(message: String?, cause: Throwable) : Error(message, cause)
