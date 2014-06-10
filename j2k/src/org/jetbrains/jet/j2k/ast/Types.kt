/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.j2k.ConverterSettings

fun Type.isPrimitive(): Boolean = this is PrimitiveType
fun Type.isUnit(): Boolean = this == Type.Unit

enum class Nullability {
    Nullable
    NotNull
    Default
}

fun Nullability.isNullable(settings: ConverterSettings) = when(this) {
    Nullability.Nullable -> true
    Nullability.NotNull -> false
    Nullability.Default -> !settings.forceNotNullTypes
}

abstract class MayBeNullableType(nullability: Nullability, val settings: ConverterSettings) : Type {
    override val isNullable: Boolean = nullability.isNullable(settings)
}

trait NotNullType : Type {
    override val isNullable: Boolean
        get() = false
}

trait Type : Element {
    val isNullable: Boolean

    open fun toNotNullType(): Type {
        if (isNullable) throw UnsupportedOperationException("toNotNullType must be defined")
        return this
    }

    open fun toNullableType(): Type {
        if (!isNullable) throw UnsupportedOperationException("toNullableType must be defined")
        return this
    }

    protected fun isNullableStr(): String? {
        return if (isNullable) "?" else ""
    }

    object Empty : NotNullType {
        override fun toKotlin(): String = "UNRESOLVED_TYPE"
    }

    object Unit: NotNullType {
        override fun toKotlin() = "Unit"
    }

    override fun equals(other: Any?): Boolean = other is Type && other.toKotlin() == this.toKotlin()

    override fun hashCode(): Int = toKotlin().hashCode()

    override fun toString(): String = toKotlin()
}

class ClassType(val `type`: Identifier, val typeArgs: List<Element>, nullability: Nullability, settings: ConverterSettings)
  : MayBeNullableType(nullability, settings) {

    override fun toKotlin(): String {
        // TODO change to map() when KT-2051 is fixed
        val parametersToKotlin = ArrayList<String>()
        for (param in typeArgs) {
            parametersToKotlin.add(param.toKotlin())
        }
        var params: String = if (parametersToKotlin.size() == 0)
            ""
        else
            "<" + parametersToKotlin.makeString(", ") + ">"
        return `type`.toKotlin() + params + isNullableStr()
    }


    override fun toNotNullType(): Type = ClassType(`type`, typeArgs, Nullability.NotNull, settings)
    override fun toNullableType(): Type = ClassType(`type`, typeArgs, Nullability.Nullable, settings)
}

class ArrayType(val elementType: Type, nullability: Nullability, settings: ConverterSettings)
  : MayBeNullableType(nullability, settings) {

    override fun toKotlin(): String {
        if (elementType is PrimitiveType) {
            return elementType.toKotlin() + "Array" + isNullableStr()
        }

        return "Array<" + elementType.toKotlin() + ">" + isNullableStr()
    }

    override fun toNotNullType(): Type = ArrayType(elementType, Nullability.NotNull, settings)
    override fun toNullableType(): Type = ArrayType(elementType, Nullability.Nullable, settings)
}

class InProjectionType(val bound: Type) : NotNullType {
    override fun toKotlin(): String = "in " + bound.toKotlin()
}

class OutProjectionType(val bound: Type) : NotNullType {
    override fun toKotlin(): String = "out " + bound.toKotlin()
}

class StarProjectionType() : NotNullType {
    override fun toKotlin(): String = "*"
}

class PrimitiveType(val `type`: Identifier) : NotNullType {
    override fun toKotlin(): String = `type`.toKotlin()
}

class VarArgType(val `type`: Type) : NotNullType {
    override fun toKotlin(): String = `type`.toKotlin()
}
