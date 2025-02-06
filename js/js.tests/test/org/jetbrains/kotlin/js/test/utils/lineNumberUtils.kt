/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.util.TextOutputImpl

fun JsProgram.toStringWithLineNumbers(): String {
    val output = TextOutputImpl()
    val lineCollector = LineCollector().also { it.accept(this) }
    LineOutputToStringVisitor(output, lineCollector).accept(this.globalBlock)
    return output.toString()
}
