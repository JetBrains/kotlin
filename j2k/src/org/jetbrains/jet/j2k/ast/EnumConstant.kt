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

import org.jetbrains.jet.j2k.ast.types.Type

public open class EnumConstant(identifier: Identifier,
                               docComments: List<Node>,
                               modifiers: Set<Modifier>,
                               `type`: Type,
                               params: Element) : Field(identifier, docComments, modifiers, `type`.convertedToNotNull(), params, 0) {

    public override fun toKotlin(): String {
        if (initializer.toKotlin().isEmpty()) {
            return identifier.toKotlin()
        }

        return identifier.toKotlin() + " : " + `type`.toKotlin() + "(" + initializer.toKotlin() + ")"
    }


}
