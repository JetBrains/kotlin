/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

interface UForExpression : ULoopExpression {
    val declaration: UExpression?
    val condition: UExpression?
    val update: UExpression?

    override fun traverse(callback: UastCallback) {
        declaration?.handleTraverse(callback)
        condition?.handleTraverse(callback)
        update?.handleTraverse(callback)
        body.handleTraverse(callback)
    }

    override fun renderString() = buildString {
        append("for (")
        declaration?.let { append(it.renderString()) }
        append("; ")
        condition?.let { append(it.renderString()) }
        append("; ")
        update?.let { append(it.renderString()) }
        append(") ")
        append(body.renderString())
    }

    override fun logString() = log("UForExpression", declaration, condition, update, body)
}
