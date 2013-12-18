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

package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.Converter

class ArrayType(
        val elementType: Type,
        nullable: Boolean,
        converter: Converter
) : MayBeNullableType(nullable, converter) {
    override fun toKotlin(): String {
        if (elementType is PrimitiveType) {
            return elementType.toKotlin() + "Array" + isNullableStr()
        }

        return "Array<" + elementType.toKotlin() + ">" + isNullableStr()
    }

    override fun convertedToNotNull(): Type = ArrayType(elementType, false, converter)
}
