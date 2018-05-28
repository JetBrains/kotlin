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
import org.jetbrains.kotlin.j2k.tree.impl.JKSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

interface JKOperator

interface JKQualifier

interface JKElement {
    val parent: JKElement?

    fun detach(from: JKElement)

    fun attach(to: JKElement)
}

interface JKBranchElement : JKElement {
    val children: List<Any>
}

interface JKModifierListOwner {
    var modifierList: JKModifierList
}

interface JKReferenceTarget {
    val valid: Boolean
}

interface JKType

interface JKClassType : JKType {
    val classReference: JKSymbol?
    val nullability: Nullability
    val parameters: List<JKType>
}

interface JKJavaPrimitiveType : JKType {
    val jvmPrimitiveType: JvmPrimitiveType
}

interface JKJavaArrayType : JKType {
    val type: JKType
}