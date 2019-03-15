/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*

fun renameLabels(root: JsNode) {
    // Labels in JS are in separate scope from local variables. It's only important that nested labels don't clash.
    // Adjacent labels in one function is OK
    root.accept(object : RecursiveJsVisitor() {
        val replacements = mutableMapOf<JsName, JsName>()

        var labels = mutableSetOf<String>()

        override fun visitElement(node: JsNode) {
            super.visitElement(node)
            if (node is HasName) {
                val name = node.name
                if (name != null) {
                    replacements[name]?.let { node.name = it }
                }
            }
        }

        override fun visitLabel(x: JsLabel) {
            var addedName: String? = null
            if (x.name.isTemporary) {
                var resolvedName = x.name.ident
                var suffix = 0
                while (!labels.add(resolvedName)) {
                    resolvedName = "${x.name.ident}_${suffix++}"
                }
                replacements[x.name] = JsDynamicScope.declareName(resolvedName)
                addedName = resolvedName
            }
            super.visitLabel(x)
            addedName?.let {
                labels.remove(it)
            }
        }

        override fun visitFunction(x: JsFunction) {
            val oldLabels = labels
            labels = mutableSetOf()
            super.visitFunction(x)
            labels = oldLabels
        }
    })
}