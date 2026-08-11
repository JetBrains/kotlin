/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.testOld.utils

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.inline.util.collectNamedFunctions

object AstSearchUtil {
    @JvmStatic
    fun getClass(searchRoot: JsNode, name: String): JsClass {
        var jsClass: JsClass? = null
        searchRoot.accept(
            object : JsVisitor() {
                override fun visitElement(node: JsNode) {
                    node.acceptChildren(this)
                }

                override fun visitClass(x: JsClass) {
                    if (x.name?.ident == name) {
                        jsClass = x
                    } else {
                        x.acceptChildren(this)
                    }
                }
            }
        )
        checkNotNull(jsClass) { "Class `" + name + "` was not found" }
        return jsClass
    }

    @JvmStatic
    fun getFunction(searchRoot: JsNode, name: String): JsFunction =
        checkNotNull(findByIdent(collectNamedFunctions(searchRoot), name)) { "Function `" + name + "` was not found" }

    @JvmStatic
    fun getFunctions(searchRoot: JsNode, name: String): List<JsFunction> {
        val functions = collectNamedFunctions(searchRoot)
        check(functions.isNotEmpty()) { "Function `" + name + "` was not found" }
        return functions.mapNotNull { [key, value] -> value.takeIf { key.ident == name } }
    }

    private fun <T : JsExpression> findByIdent(properties: Map<JsName, T>, name: String): T? {
        for ([key, value] in properties) {
            if (key.ident == name) {
                return value
            }
        }

        return null
    }
}
