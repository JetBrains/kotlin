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

import org.jetbrains.jet.j2k.*

class Enum(
        name: Identifier,
        annotations: Annotations,
        modifiers: Modifiers,
        typeParameterList: TypeParameterList,
        extendsTypes: List<Type>,
        baseClassParams: List<Expression>,
        implementsTypes: List<Type>,
        body: ClassBody
) : Class(name, annotations, modifiers, typeParameterList,
          extendsTypes, baseClassParams, implementsTypes, body) {

    override fun generateCode(builder: CodeBuilder) {
        builder.append(body.factoryFunctions, "\n", "", "\n\n")

        builder append annotations appendWithSpaceAfter presentationModifiers() append "enum class " append name

        if (body.primaryConstructorSignature != null) {
            builder.append(body.primaryConstructorSignature)
        }

        builder append typeParameterList

        appendBaseTypes(builder)

        body.append(builder)
    }
}