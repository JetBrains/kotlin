/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor

fun GeneratedCode.Builder.inlineCode(string: String) = +InlineCode(string)

private data class InlineCode(val string: String) : InternalGeneratedCode() {
    override fun GeneratedCodeVisitor.visit(indent: Int) {
        writeScript {
            appendLine(string, indent)
        }
    }
}