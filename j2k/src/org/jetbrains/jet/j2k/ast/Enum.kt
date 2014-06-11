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

class Enum(
        name: Identifier,
        comments: MemberComments,
        annotations: Annotations,
        modifiers: Set<Modifier>,
        typeParameterList: TypeParameterList,
        extendsTypes: List<Type>,
        baseClassParams: List<Expression>,
        implementsTypes: List<Type>,
        body: ClassBody
) : Class(name, comments, annotations, modifiers, typeParameterList,
          extendsTypes, baseClassParams, implementsTypes, body) {

    override fun primaryConstructorSignatureToKotlin(): String
        = body.primaryConstructor?.signatureToKotlin() ?: ""

    override fun toKotlin(): String {
        return commentsToKotlin() +
                annotations.toKotlin() +
                modifiersToKotlin() +
                "enum class " + name.toKotlin() +
                primaryConstructorSignatureToKotlin() +
                typeParameterList.toKotlin() +
                implementTypesToKotlin() +
                body.toKotlin(this)
    }
}