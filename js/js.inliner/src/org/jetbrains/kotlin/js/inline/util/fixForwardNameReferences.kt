/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.inline.util

import org.jetbrains.kotlin.js.backend.ast.*

fun JsNode.fixForwardNameReferences() {
    accept(object : RecursiveJsVisitor() {
        val currentScope = mutableMapOf<String, JsName>()

        init {
            currentScope += collectDefinedNames(this@fixForwardNameReferences, skipLabelsAndCatches = true).associateBy { it.ident }
        }

        private fun restore(ident: String, oldName: JsName?) {
            if (oldName == null) {
                currentScope -= ident
            } else {
                currentScope[ident] = oldName
            }
        }

        override fun visitFunction(x: JsFunction) {
            val localVars = x.collectLocalVariables(skipLabelsAndCatches = true).toList()
            val backup = arrayOfNulls<JsName>(localVars.size)

            localVars.forEachIndexed { index, localVar ->
                backup[index] = currentScope[localVar.ident]
                currentScope[localVar.ident] = localVar
            }

            super.visitFunction(x)

            for (index in localVars.indices.reversed()) {
                restore(localVars[index].ident, backup[index])
            }
        }

        override fun visitCatch(x: JsCatch) {
            val name = x.parameter.name
            val oldName = currentScope[name.ident]
            currentScope[name.ident] = name

            super.visitCatch(x)

            restore(name.ident, oldName)
        }

        override fun visitNameRef(nameRef: JsNameRef) {
            super.visitNameRef(nameRef)
            if (nameRef.qualifier == null) {
                val ident = nameRef.ident
                val name = currentScope[ident]
                if (name != null) {
                    nameRef.name = name
                }
            }
        }

        override fun visitBreak(x: JsBreak) {}

        override fun visitContinue(x: JsContinue) {}
    })
}