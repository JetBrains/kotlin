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

interface JKOperator: JKTreeElement

interface JKQualifier

interface JKElement {
    var parent: JKElement?
}

interface JKBranchElement : JKElement {
    val children: List<JKTreeElement>
}

interface JKModifierListOwner {
    var modifierList: JKModifierList
}

interface JKReferenceTarget {
    val valid: Boolean
}

interface JKDeclaration : JKElement, JKReferenceTarget

interface JKClass : JKDeclaration, JKModifierListOwner {
    val name: JKNameIdentifier
    val declarationList: JKDeclarationList
    var classKind: ClassKind

    enum class ClassKind {
        ABSTRACT, ANNOTATION, CLASS, ENUM, INTERFACE
    }
}

interface JKMethod : JKDeclaration, JKModifierListOwner {
    val name: JKNameIdentifier
    var valueArguments: List<JKValueArgument>
    val returnType: JKType
}

interface JKField : JKDeclaration, JKModifierListOwner {
    val type: JKType
    val name: JKNameIdentifier
}

interface JKSymbol<E : JKElement> {
    val element: E
}
