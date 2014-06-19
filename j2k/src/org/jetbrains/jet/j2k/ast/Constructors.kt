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

import java.util.HashSet
import java.util.ArrayList
import org.jetbrains.jet.j2k.*

abstract class Constructor(
        converter: Converter,
        annotations: Annotations,
        modifiers: Set<Modifier>,
        parameterList: ParameterList,
        block: Block
) : Function(converter, Identifier.Empty, annotations, modifiers, Type.Empty, TypeParameterList.Empty, parameterList, block, false)

class PrimaryConstructor(converter: Converter,
                         annotations: Annotations,
                         modifiers: Set<Modifier>,
                         parameterList: ParameterList,
                         block: Block)
  : Constructor(converter, annotations, modifiers, parameterList, block) {

    public fun appendSignature(builder: CodeBuilder): CodeBuilder {
        val accessModifier = modifiers.accessModifier()
        val modifiersString = if (accessModifier != null && accessModifier != Modifier.PUBLIC) " " + accessModifier.toKotlin() else ""
        return builder.append(modifiersString).append("(").append(parameterList).append(")")
    }

    public fun appendBody(builder: CodeBuilder): CodeBuilder = builder.append(block!!)
}

class SecondaryConstructor(converter: Converter,
                         annotations: Annotations,
                         modifiers: Set<Modifier>,
                         parameterList: ParameterList,
                         block: Block)
  : Constructor(converter, annotations, modifiers, parameterList, block) {

    public fun toFactoryFunction(containingClass: Class?): Function {
        val modifiers = HashSet(modifiers)
        val statements = ArrayList(block?.statements ?: listOf())
        statements.add(ReturnStatement(tempValIdentifier))
        val block = Block(statements, block?.lBrace ?: LBrace(), block?.rBrace ?: RBrace())
        val typeParameters = ArrayList<TypeParameter>()
        if (containingClass != null) {
            typeParameters.addAll(containingClass.typeParameterList.parameters)
        }
        return Function(converter, Identifier("create"), annotations, modifiers,
                        ClassType(containingClass?.name ?: Identifier.Empty, typeParameters, Nullability.NotNull, converter.settings),
                        TypeParameterList(typeParameters), parameterList, block, false).assignPrototypesFrom(this)
    }

    class object {
        public val tempValIdentifier: Identifier = Identifier("__", false)
    }
}
