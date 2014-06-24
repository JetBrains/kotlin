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

abstract class Constructor(
        converter: Converter,
        annotations: Annotations,
        modifiers: Modifiers,
        parameterList: ParameterList,
        block: Block
) : Function(converter, Identifier.Empty, annotations, modifiers, ErrorType(), TypeParameterList.Empty, parameterList, block, false)

class PrimaryConstructor(converter: Converter,
                         annotations: Annotations,
                         modifiers: Modifiers,
                         parameterList: ParameterList,
                         block: Block)
  : Constructor(converter, annotations, modifiers, parameterList, block) {

    public fun appendSignature(builder: CodeBuilder): CodeBuilder {
        val accessModifier = modifiers.filter { it in ACCESS_MODIFIERS && it != Modifier.PUBLIC }
        if (!accessModifier.isEmpty) {
            builder append " " append accessModifier
        }
        return builder append "(" append parameterList append ")"
    }

    public fun appendBody(builder: CodeBuilder): CodeBuilder = builder.append(block!!)
}

class SecondaryConstructor(converter: Converter,
                         annotations: Annotations,
                         modifiers: Modifiers,
                         parameterList: ParameterList,
                         block: Block)
  : Constructor(converter, annotations, modifiers, parameterList, block) {

    public fun toFactoryFunction(containingClass: Class?): Function {
        val statements = ArrayList(block?.statements ?: listOf())
        statements.add(ReturnStatement(tempValIdentifier()).assignNoPrototype())
        val newBlock = Block(statements, block?.lBrace ?: LBrace().assignNoPrototype(), block?.rBrace ?: RBrace().assignNoPrototype())
        if (this.block != null) {
            newBlock.assignPrototypesFrom(block!!)
        }

        val typeParameters = if (containingClass != null) containingClass.typeParameterList.parameters else listOf()
        val typeParameterList = TypeParameterList(typeParameters).assignNoPrototype()

        val returnType = ClassType(containingClass?.name ?: Identifier.Empty, typeParameters, Nullability.NotNull, converter.settings).assignNoPrototype()
        return Function(converter, Identifier("create").assignNoPrototype(), annotations, modifiers,
                        returnType, typeParameterList, parameterList, newBlock, false).assignPrototypesFrom(this)
    }

    class object {
        public val tempValName: String = "__"
        public fun tempValIdentifier(): Identifier = Identifier(tempValName, false).assignNoPrototype()
    }
}
