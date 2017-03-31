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

package org.jetbrains.kotlin.js.dce

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.dce.Context.Node

fun Context.isObjectDefineProperty(function: JsExpression) = isObjectFunction(function, "defineProperty")

fun Context.isObjectGetOwnPropertyDescriptor(function: JsExpression) = isObjectFunction(function, "getOwnPropertyDescriptor")

fun Context.isDefineModule(function: JsExpression): Boolean = isKotlinFunction(function, "defineModule")

fun Context.isDefineInlineFunction(function: JsExpression): Boolean = isKotlinFunction(function, "defineInlineFunction")

fun Context.isObjectFunction(function: JsExpression, functionName: String): Boolean {
    if (function !is JsNameRef) return false
    if (function.ident != functionName) return false

    val receiver = function.qualifier as? JsNameRef ?: return false
    if (receiver.name?.let { nodes[it] } != null) return false

    return receiver.ident == "Object"
}

fun Context.isKotlinFunction(function: JsExpression, name: String): Boolean {
    if (function !is JsNameRef || function.ident != name) return false
    val receiver = (function.qualifier as? JsNameRef)?.name ?: return false
    return receiver in nodes && receiver.ident.toLowerCase() == "kotlin"
}

fun Context.isAmdDefine(function: JsExpression): Boolean = isTopLevelFunction(function, "define")

fun Context.isTopLevelFunction(function: JsExpression, name: String): Boolean {
    if (function !is JsNameRef || function.qualifier != null) return false
    return function.ident == name && function.name !in nodes.keys
}

fun JsNode.extractLocation(): JsLocation? {
    return when (this) {
        is SourceInfoAwareJsNode -> source as? JsLocation
        is JsExpressionStatement -> expression.source as? JsLocation
        else -> null
    }
}

fun JsLocation.asString(): String {
    val simpleFileName = file.substring(file.lastIndexOf("/") + 1)
    return "$simpleFileName:${startLine + 1}"
}

fun Set<Node>.extractRoots(): Set<Node> {
    val result = mutableSetOf<Node>()
    val visited = mutableSetOf<Node>()
    forEach { it.original.extractRootsImpl(result, visited) }
    return result
}

private fun Node.extractRootsImpl(target: MutableSet<Node>, visited: MutableSet<Node>) {
    if (!visited.add(original)) return
    val qualifier = original.qualifier
    if (qualifier == null) {
        target += original
    }
    else {
        qualifier.parent.extractRootsImpl(target, visited)
    }
}
