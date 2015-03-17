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

import org.jetbrains.kotlin.j2k.*

open class Class(
        val name: Identifier,
        annotations: Annotations,
        modifiers: Modifiers,
        val typeParameterList: TypeParameterList,
        val extendsTypes: List<Type>,
        val baseClassParams: List<DeferredElement<Expression>>,
        val implementsTypes: List<Type>,
        val body: ClassBody
) : Member(annotations, modifiers) {

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendWithSpaceAfter(presentationModifiers())
                .append(keyword)
                .append(" ")
                .append(name)
                .append(typeParameterList)

        if (body.primaryConstructorSignature != null) {
            builder.append(body.primaryConstructorSignature)
        }

        appendBaseTypes(builder)
        typeParameterList.appendWhere(builder)

        body.append(builder)
    }

    protected open val keyword: String
        get() = "class"

    protected fun appendBaseTypes(builder: CodeBuilder) {
        builder.append(baseClassSignatureWithParams(builder) + implementsTypes.map { { builder.append(it) } }, ", ", ":")
    }

    private fun baseClassSignatureWithParams(builder: CodeBuilder): List<() -> CodeBuilder> {
        if (keyword.equals("class") && extendsTypes.size() == 1) {
            return listOf({
                              builder append extendsTypes[0] append "("
                              builder.append(baseClassParams, ", ")
                              builder append ")"
                          })
        }
        return extendsTypes.map { { builder.append(it) } }
    }

    protected open fun presentationModifiers(): Modifiers
            = if (modifiers.contains(Modifier.ABSTRACT)) modifiers.without(Modifier.OPEN) else modifiers
}
