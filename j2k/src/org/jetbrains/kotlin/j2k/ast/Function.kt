/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.CodeBuilder

abstract class FunctionLike(
        annotations: Annotations,
        modifiers: Modifiers,
        open val parameterList: ParameterList?,
        val body: DeferredElement<Block>?
) : Member(annotations, modifiers) {

    protected open fun presentationModifiers(): Modifiers = modifiers
}

class Function(
        val name: Identifier,
        annotations: Annotations,
        modifiers: Modifiers,
        val returnType: Type,
        val typeParameterList: TypeParameterList,
        parameterList: ParameterList,
        body: DeferredElement<Block>?,
        private val isInInterface: Boolean
) : FunctionLike(annotations, modifiers, parameterList, body) {

    override val parameterList: ParameterList
        get() = super.parameterList!!

    override fun presentationModifiers(): Modifiers {
        var modifiers = this.modifiers
        if (isInInterface) {
            modifiers = modifiers.without(Modifier.ABSTRACT)
        }

        if (modifiers.contains(Modifier.OVERRIDE)) {
            modifiers = modifiers.filter { it != Modifier.OPEN }
        }

        return modifiers
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendWithSpaceAfter(presentationModifiers())
                .append("fun ")
                .appendWithSuffix(typeParameterList, " ")
                .append(name)
                .append(parameterList)

        if (!returnType.isUnit()) {
            builder append ":" append returnType
        }

        typeParameterList.appendWhere(builder)

        if (body != null) {
            builder append " " append body
        }
    }
}

class AnonymousFunction(
        returnType: Type,
        typeParameterList: TypeParameterList,
        parameterList: ParameterList,
        body: DeferredElement<Block>?
): Expression() {
    private val function = Function(Identifier.Empty, Annotations.Empty, Modifiers.Empty, returnType, typeParameterList, parameterList, body, false)

    override fun generateCode(builder: CodeBuilder) = function.generateCode(builder)
}
