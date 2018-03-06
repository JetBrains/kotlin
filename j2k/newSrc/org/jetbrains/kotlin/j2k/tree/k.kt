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

interface JKKtFun : JKDeclaration

interface JKKtConstructor : JKDeclaration

interface JKKtPrimaryConstructor : JKKtConstructor

interface JKKtAssignmentStatement : JKStatement

interface JKKtCall : JKMethodCallExpression

interface JKKtProperty : JKDeclaration, JKModifierListOwner {
    val type: JKType
    val name: JKNameIdentifier
    val initializer: JKExpression?
    val getter: JKBlock?
    val setter: JKBlock?
}

interface JKKtFunction : JKDeclaration, JKModifierListOwner {
    val returnType: JKType
    val name: JKNameIdentifier
    val valueArguments: List<JKValueArgument>
    val block: JKBlock?
}
interface JKKtModifier : JKModifier {
    val type: KtModifierType

    enum class KtModifierType {
        ACTUAL, ABSTRACT, ANNOTATION, COMPANION, CONST, CROSSINLINE, DATA, ENUM, EXPECT, EXTERNAL, FINAL, INFIX, INLINE, INNER,
        INTERNAL, LATEINIT, NOINLINE, OPEN, OPERATOR, OUT, OVERRIDE, REIFIED, SEALED, SUSPEND, TAILREC, VARARG
    }
}