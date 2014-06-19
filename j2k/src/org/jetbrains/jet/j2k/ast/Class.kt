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

import java.util.ArrayList
import org.jetbrains.jet.j2k.*

open class Class(
        val name: Identifier,
        annotations: Annotations,
        modifiers: Set<Modifier>,
        val typeParameterList: TypeParameterList,
        val extendsTypes: List<Type>,
        val baseClassParams: List<Expression>,
        val implementsTypes: List<Type>,
        val body: ClassBody
) : Member(annotations, modifiers) {

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
        appendModifiers(builder).append(keyword).append(" ").append(name).append(typeParameterList)
        appendPrimaryConstructorSignature(builder)
        appendBaseTypes(builder)
        typeParameterList.appendWhere(builder)
        body.append(builder, this)
    }

    protected open val keyword: String
        get() = "class"

    protected open fun appendPrimaryConstructorSignature(builder: CodeBuilder) {
        body.primaryConstructor?.appendSignature(builder) ?: builder.append("()")
    }

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

    protected open fun appendModifiers(builder: CodeBuilder): CodeBuilder {
        val modifierList = ArrayList<Modifier>()

        modifiers.accessModifier()?.let { modifierList.add(it) }

        if (modifiers.contains(Modifier.ABSTRACT)) {
            modifierList.add(Modifier.ABSTRACT)
        }
        else if (modifiers.contains(Modifier.OPEN)) {
            modifierList.add(Modifier.OPEN)
        }

        if (modifiers.contains(Modifier.INNER)) {
            modifierList.add(Modifier.INNER)
        }

        return builder.append(modifierList)
    }
}
