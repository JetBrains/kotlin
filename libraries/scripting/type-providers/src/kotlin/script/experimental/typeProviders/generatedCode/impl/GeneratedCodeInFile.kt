/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import java.io.File
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode

internal fun File.asGeneratedCode(): GeneratedCode =
    GeneratedCodeInFile(this)

private class GeneratedCodeInFile(val file: File) : InternalGeneratedCode() {
    init {
        require(file.exists()) { "Provided file does not exist" }
    }

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        includeScript(file.toScriptSource())
    }
}
