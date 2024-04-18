/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

// crypto.randomUUID() is supported from Node.js version 19.0.0 (Released 2022-10-18)
// Currently, WasmJs supports only the latest Node.js.
// TODO: Consider using crypto.getRandomValues()
//   Encountered difficulties with passing and retrieving ByteArray using js()
private fun jsRandomUUID(): String =
    js("crypto.randomUUID()")

internal actual fun secureRandomUUID(): UUID {
    val uuidString = jsRandomUUID()
    return UUID.parse(uuidString)
}

