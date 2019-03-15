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
import org.jetbrains.kotlin.js.backend.ast.metadata.imported


fun removeUnusedImports(fragment: JsProgramFragment, code: JsBlock) {
    val usedImports = mutableSetOf<JsName>()

    collectUsedImports(code, usedImports)

    fragment.nameBindings.retainAll { !it.name.imported || it.name in usedImports }

    val existingTags = fragment.nameBindings.map { it.key }.toSet()

    fragment.imports.entries.retainAll { (k, _) -> k in existingTags }
}

private fun collectUsedImports(root: JsNode, to: MutableSet<JsName>): Set<JsName> {
    val collector = UsedImportsCollector(to)
    root.accept(collector)

    // See StaticContext.getVariableForPropertyMetadata
    // TODO Find a better way
    val removedPseudoImports = mutableSetOf<JsName>()
    NodeRemover(JsVars::class.java) { statement ->
        if (statement.vars.size == 1) {
            val name = statement.vars[0].name
            (name.imported && name !in collector.usedImports).also {
                if (it) removedPseudoImports += name
            }
        } else {
            false
        }
    }.accept(root)
    collector.pseudoImports.forEach {
        if (it.name !in removedPseudoImports) {
            it.initExpression.accept(collector)
        }
    }

    return collector.usedImports
}

private class UsedImportsCollector(val usedImports: MutableSet<JsName>) : RecursiveJsVisitor() {

    val pseudoImports = mutableListOf<JsVars.JsVar>()

    override fun visit(x: JsVars.JsVar) {
        if (x.name.imported) {
            pseudoImports += x
        } else {
            super.visit(x)
        }
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        val name = nameRef.name
        if (name != null && name.imported) {
            usedImports += name
        }
        super.visitNameRef(nameRef)
    }

    override fun visitFunction(x: JsFunction) {
        x.coroutineMetadata?.apply {
            accept(suspendObjectRef)
            accept(baseClassRef)
        }
        super.visitFunction(x)
    }
}