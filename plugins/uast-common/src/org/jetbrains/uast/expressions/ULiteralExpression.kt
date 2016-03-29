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

interface ULiteralExpression : UExpression {
    val text: String

    val value: Any?

    val isNull: Boolean

    val isString: Boolean
        get() = evaluate() is String

    val isBoolean: Boolean
        get() = evaluate() is Boolean

    override fun traverse(callback: UastCallback) {}
    override fun logString() = "ULiteralExpression ($text)"
    override fun renderString() = text
}
