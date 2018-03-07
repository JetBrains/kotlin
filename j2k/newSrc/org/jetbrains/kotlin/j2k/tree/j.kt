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
    val type: JKType
    val name: JKNameIdentifier
    val initializer: JKExpression?
}

interface JKJavaMethod : JKMethod

interface JKJavaForLoop : JKLoop

interface JKJavaAssignmentExpression : JKExpression

interface JKJavaPrimitiveType : JKType {
    val name: String
}

interface JKJavaArrayType : JKType {
    val type: JKType
}

interface JKJavaStringLiteralExpression : JKLiteralExpression {
    val text: String
}

interface JKJavaOperatorIdentifier : JKOperatorIdentifier

interface JKJavaQualificationIdentifier : JKQualificationIdentifier

interface JKJavaMethodCallExpression : JKMethodCallExpression {

}

interface JKJavaFieldAccessExpression : JKFieldAccessExpression {

}

interface JKJavaNewExpression : JKExpression {
    val identifier: JKClassReference
    val arguments: JKExpressionList
}

interface JKJavaMethodReference : JKMethodReference

interface JKJavaFieldReference : JKFieldReference

interface JKJavaClassReference : JKClassReference

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

interface JKJavaNewEmptyArray : JKExpression {
    val initializer: List<JKLiteralExpression?>
}

interface JKJavaNewArray : JKExpression {
    val initializer: List<JKExpression>
}
