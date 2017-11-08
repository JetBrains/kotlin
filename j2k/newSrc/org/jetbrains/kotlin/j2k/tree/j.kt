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

interface JKJavaField : JKDeclaration, JKModifierListOwner {
    var type: JKTypeIdentifier
    var name: JKNameIdentifier
    var initializer: JKExpression?
}

interface JKJavaMethod : JKDeclaration {
    var modifierList: JKModifierList
    var name: JKNameIdentifier
    var valueArguments: List<JKValueArgument>
    var block: JKBlock?
}

interface JKJavaForLoop : JKLoop

interface JKJavaAssignmentExpression : JKExpression

interface JKJavaCall : JKCall

interface JKJavaTypeIdentifier : JKTypeIdentifier {
    val typeName: String
}

interface JKJavaStringLiteralExpression : JKLiteralExpression {
    val text: String
}

interface JKJavaAccessModifier : JKAccessModifier {
    val type: AccessModifierType

    enum class AccessModifierType {
        PUBLIC, PROTECTED, PRIVATE
    }
}

interface JKJavaModifier : JKModifier {
    val type: JavaModifierType

    enum class JavaModifierType {
        ABSTRACT, FINAL, NATIVE, STATIC, STRICTFP, SYNCHRONIZED, TRANSIENT, VOLATILE
    }
}