/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor

/**
 * Run some code when on start up of the script
 */
fun GeneratedCode.Builder.runOnStart(code: () -> Unit) {
    +RunCode(code)
}

private class RunCode(
    val function: () -> Unit
) : InternalGeneratedCode() {

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        withSerialized(function) { id ->
            writeScript {
                appendLine("fun onStart$id() {", indent)
                appendLine(
                    "val function: () -> Unit = __Deserialization__.unsafeReadSerializedValue(\"$id\")",
                    indent + 1
                )
                appendLine("function()", indent + 1)
                appendLine("}", indent)
                appendLine("onStart$id()", indent)
            }
        }
    }

}
