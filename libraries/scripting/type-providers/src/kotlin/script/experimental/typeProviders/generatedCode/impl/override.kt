/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.leaves
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor

interface OverridableBuilder<out Overridden> {
    fun override(init: Overridden.() -> Unit)
}

internal open class OverrideBuilder(
    val builder: GeneratedCode.Builder
) : GeneratedCode.Builder {

    override fun GeneratedCode.unaryPlus() {
        with(builder) {
            for (leaf in leaves()) {
                if (leaf.isOverridable) {
                    +OverrideCode(leaf)
                } else {
                    +leaf
                }
            }
        }
    }
}

private class OverrideCode(val code: InternalGeneratedCode) : InternalGeneratedCode() {
    override fun GeneratedCodeVisitor.visit(indent: Int) {
        writeScript {
            append("override ", indent)
            visit(code, indent)
        }
    }
}