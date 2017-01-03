/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.inline.util.collectReferencedTemporaryNames

fun JsNode.resolveTemporaryNames() {
    val allNames = collectReferencedTemporaryNames(this)

    val scopeTree = ScopeCollector().let {
        it.accept(this)
        it.scopeTree
    }

    val renamings = scopeTree.resolveNames(allNames)
    accept(object : RecursiveJsVisitor() {
        override fun visitElement(node: JsNode) {
            super.visitElement(node)
            if (node is HasName) {
                val name = node.name
                if (name != null) {
                    renamings[name]?.let { node.name = it }
                }
            }
        }

        override fun visitFunction(x: JsFunction) {
            x.coroutineMetadata?.apply {
                accept(suspendObjectRef)
                accept(baseClassRef)
            }
            super.visitFunction(x)
        }
    })
}

private fun Map<JsScope, Set<JsScope>>.resolveNames(knownNames: Set<JsName>): Map<JsName, JsName> {
    val replacements = mutableMapOf<JsName, JsName>()
    fun traverse(scope: JsScope) {
        for (temporaryName in scope.temporaryNames.filter { it in knownNames }.sortedBy { it.ordinal }) {
            replacements[temporaryName] = scope.declareFreshName(temporaryName.ident).apply { copyMetadataFrom(temporaryName) }
        }
        this[scope]!!.forEach(::traverse)
    }

    val roots = keys.toMutableSet()
    values.forEach { roots -= it }

    for (root in roots) {
        traverse(root)
    }

    return replacements
}

private class ScopeCollector : RecursiveJsVisitor() {
    val scopeTree = mutableMapOf<JsScope, MutableSet<JsScope>>()

    override fun visitCatch(x: JsCatch) {
        recordScope(x.scope)
        super.visitCatch(x)
    }

    override fun visitFunction(x: JsFunction) {
        recordScope(x.scope)
        super.visitFunction(x)
    }
    override fun visitElement(node: JsNode) {
        if (node is HasName) {
            node.name?.let { recordScope(it.enclosing) }
        }
        super.visitElement(node)
    }

    private fun recordScope(scope: JsScope) {
        if (scope !in scopeTree) {
            scopeTree[scope] = mutableSetOf()
            val parent = scope.parent
            if (parent != null) {
                recordScope(parent)
                scopeTree[parent]!!.add(scope)
            }
        }
    }
}