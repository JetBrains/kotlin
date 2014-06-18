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

import org.jetbrains.jet.j2k.ConverterSettings
import org.jetbrains.jet.j2k.CommentsAndSpaces

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

abstract class MayBeNullableType(nullability: Nullability, val settings: ConverterSettings) : Type() {
    override val isNullable: Boolean = nullability.isNullable(settings)

    protected val isNullableStr: String
        get() = if (isNullable) "?" else ""
}

abstract class NotNullType() : Type() {
    override val isNullable: Boolean
        get() = false
}

abstract class Type() : Element() {
    abstract val isNullable: Boolean

    open fun toNotNullType(): Type {
        if (isNullable) throw UnsupportedOperationException("toNotNullType must be defined")
        return this
    }

    open fun toNullableType(): Type {
        if (!isNullable) throw UnsupportedOperationException("toNullableType must be defined")
        return this
    }

    object Empty : NotNullType() {
        override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = "UNRESOLVED_TYPE"
    }

    object Unit: NotNullType() {
        override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "Unit"
    }

    override fun equals(other: Any?): Boolean = other is Type && other.toKotlin(CommentsAndSpaces.None) == this.toKotlin(CommentsAndSpaces.None)

    override fun hashCode(): Int = toKotlin(CommentsAndSpaces.None).hashCode()

    override fun toString(): String = toKotlin(CommentsAndSpaces.None)
}

class ClassType(val `type`: Identifier, val typeArgs: List<Element>, nullability: Nullability, settings: ConverterSettings)
  : MayBeNullableType(nullability, settings) {

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        var params = if (typeArgs.isEmpty()) "" else typeArgs.map { it.toKotlin(commentsAndSpaces) }.makeString(", ", "<", ">")
        return `type`.toKotlin(commentsAndSpaces) + params + isNullableStr
    }


    override fun toNotNullType(): Type = ClassType(`type`, typeArgs, Nullability.NotNull, settings)
    override fun toNullableType(): Type = ClassType(`type`, typeArgs, Nullability.Nullable, settings)
}

class ArrayType(val elementType: Type, nullability: Nullability, settings: ConverterSettings)
  : MayBeNullableType(nullability, settings) {

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        if (elementType is PrimitiveType) {
            return elementType.toKotlin(commentsAndSpaces) + "Array" + isNullableStr
        }

        return "Array<" + elementType.toKotlin(commentsAndSpaces) + ">" + isNullableStr
    }

    override fun toNotNullType(): Type = ArrayType(elementType, Nullability.NotNull, settings)
    override fun toNullableType(): Type = ArrayType(elementType, Nullability.Nullable, settings)
}

class InProjectionType(val bound: Type) : NotNullType() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = "in " + bound.toKotlin(commentsAndSpaces)
}

class OutProjectionType(val bound: Type) : NotNullType() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = "out " + bound.toKotlin(commentsAndSpaces)
}

class StarProjectionType() : NotNullType() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = "*"
}

class PrimitiveType(val `type`: Identifier) : NotNullType() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = `type`.toKotlin(commentsAndSpaces)
}

class VarArgType(val `type`: Type) : NotNullType() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String = `type`.toKotlin(commentsAndSpaces)
}
