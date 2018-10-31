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

package org.jetbrains.kotlin.j2k.tree

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

interface JKOperator {
    val token: JKOperatorToken
    val precedence: Int
}

interface JKOperatorToken {
    val text: String
}

interface JKKtOperatorToken : JKOperatorToken {
    val operatorName: String
}

interface JKQualifier

interface JKElement {
    val parent: JKElement?

    fun detach(from: JKElement)

    fun attach(to: JKElement)
}

interface JKBranchElement : JKElement {
    val children: List<Any>

    val valid: Boolean
    fun invalidate()
}

interface JKModifierListOwner {
    var modifierList: JKModifierList
}

interface JKType {
    val nullability: Nullability
}

interface JKParametrizedType : JKType {
    val parameters: List<JKType>
}

interface JKClassType : JKParametrizedType {
    val classReference: JKClassSymbol
    override val nullability: Nullability
}

interface JKJavaPrimitiveType : JKType {
    val jvmPrimitiveType: JvmPrimitiveType
    override val nullability: Nullability
        get() = Nullability.NotNull
}

interface JKJavaArrayType : JKType {
    val type: JKType
}

interface JKStarProjectionType : JKType {
    override val nullability: Nullability
        get() = Nullability.NotNull
}

inline fun <reified T> JKElement.getParentOfType(): T? {
    var p = parent
    while (true) {
        if (p is T || p == null)
            return p as? T
        p = p.parent
    }
}

fun <T : JKElement> T.detached() =
    also { if (parent != null) detach(parent!!) }
