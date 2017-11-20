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

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.inline.util.getImportTag
import org.jetbrains.kotlin.js.inline.util.replaceNames

fun removeDuplicateImports(node: JsNode) {
    node.accept(object : RecursiveJsVisitor() {
        override fun visitBlock(x: JsBlock) {
            super.visitBlock(x)
            removeDuplicateImports(x.statements)
        }
    })
}

private fun removeDuplicateImports(statements: MutableList<JsStatement>) {
    val existingImports = mutableMapOf<String, JsName>()
    val replacements = mutableMapOf<JsName, JsExpression>()
    removeDuplicateImports(statements, existingImports, replacements)

    for (statement in statements) {
        replaceNames(statement, replacements)
    }
}

private fun removeDuplicateImports(
    statements: MutableList<JsStatement>,
    existingImports: MutableMap<String, JsName>,
    replacements: MutableMap<JsName, JsExpression>
) {
    var index = 0
    while (index < statements.size) {
        val statement = statements[index]
        if (statement is JsVars) {
            val importTag = getImportTag(statement)
            if (importTag != null) {
                val name = statement.vars[0].name
                val existingName = existingImports[importTag]
                if (existingName != null) {
                    replacements[name] = existingName.makeRef()
                    statements.removeAt(index)
                    continue
                }
                else {
                    existingImports[importTag] = name
                }
            }
        }
        else if (statement is JsBlock) {
            removeDuplicateImports(statement.statements, existingImports, replacements)
        }

        index++
    }
}
