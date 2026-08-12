/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.util.collectors

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsObjectLiteral
import org.jetbrains.kotlin.js.backend.ast.RecursiveJsVisitor
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

internal class InstanceCollector<T : JsNode>(val klass: KClass<T>, val visitNestedDeclarations: Boolean) : RecursiveJsVisitor() {
    val collected: MutableList<T> = ArrayList()

    override fun visitFunction(x: JsFunction) {
        if (visitNestedDeclarations) {
            visitElement(x)
        }
    }

    override fun visitObjectLiteral(x: JsObjectLiteral) {
        if (visitNestedDeclarations) {
            visitElement(x)
        }
    }

    override fun visitElement(node: JsNode) {
        collected.addIfNotNull(klass.safeCast(node))
        super.visitElement(node)
    }
}
