/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.annotations.Nullable

public open class NewClassExpression(val name: Element,
                                     val arguments: List<Expression>,
                                     val qualifier: Expression = Expression.EMPTY_EXPRESSION,
                                     val anonymousClass: AnonymousClass? = null): Expression() {
    public override fun toKotlin(): String {
        val callOperator: String? = (if (qualifier.isNullable())
            "?."
        else
            ".")
        val qualifier: String? = (if (qualifier.isEmpty())
            ""
        else
            qualifier.toKotlin() + callOperator)
        val appliedArguments: String = arguments.toKotlin(", ")
        return (if (anonymousClass != null)
            "object : " + qualifier + name.toKotlin() + "(" + appliedArguments + ")" + anonymousClass.toKotlin()
        else
            qualifier + name.toKotlin() + "(" + appliedArguments + ")")
    }
}
