/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.matrix.impl

import org.jetbrains.kotlinx.serialization.matrix.FunctionContext

internal fun Appendable.writeFunction(signature: String, builder: FunctionContext.() -> Unit) {
    appendLine("fun $signature {")

    val context = object : FunctionContext {
        override fun line(code: String) {
            append("    ")
            appendLine(code)
        }
    }

    context.builder()

    appendLine("}")
    appendLine()
}