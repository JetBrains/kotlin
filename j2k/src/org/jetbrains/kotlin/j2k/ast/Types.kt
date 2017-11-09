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

import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.ConverterSettings

fun Type.isUnit(): Boolean = this is UnitType

enum class Nullability {
    Nullable,
    NotNull,
    Default
}

fun Nullability.isNullable(settings: ConverterSettings) = when(this) {
    Nullability.Nullable -> true
    Nullability.NotNull -> false
    Nullability.Default -> !settings.forceNotNullTypes
}

enum class Mutability {
    Mutable,
    NonMutable,
    Default
}

fun Mutability.isMutable(settings: ConverterSettings) = when(this) {
    Mutability.Mutable -> true
    Mutability.NonMutable -> false
    Mutability.Default -> false //TODO: setting?
}

abstract class MayBeNullableType(nullability: Nullability, val settings: ConverterSettings) : Type() {
    override val isNullable: Boolean = nullability.isNullable(settings)

    protected val isNullableStr: String
        get() = if (isNullable) "?" else ""
}

abstract class NotNullType : Type() {
    override val isNullable: Boolean
        get() = false
}

abstract class Type : Element() {
    abstract val isNullable: Boolean

    open fun toNotNullType(): Type = this

    open fun toNullableType(): Type = this

    override fun equals(other: Any?): Boolean = other is Type && other.canonicalCode() == this.canonicalCode()

    override fun hashCode(): Int = canonicalCode().hashCode()

    override fun toString(): String = canonicalCode()
}

class UnitType : NotNullType() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("Unit")
    }
}

open class ErrorType : Type() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("???")
    }

    override val isNullable: Boolean
        get() = false
}

class NullType : ErrorType() {
    override val isNullable: Boolean
        get() = true
}

class ClassType(val referenceElement: ReferenceElement, nullability: Nullability, settings: ConverterSettings)
  : MayBeNullableType(nullability, settings) {

    override fun generateCode(builder: CodeBuilder) {
        builder append referenceElement append isNullableStr
    }

    override fun toNotNullType(): Type = ClassType(referenceElement, Nullability.NotNull, settings).assignPrototypesFrom(this)
    override fun toNullableType(): Type = ClassType(referenceElement, Nullability.Nullable, settings).assignPrototypesFrom(this)

    fun isAnonymous() = referenceElement.name.isEmpty
}

class ArrayType(val elementType: Type, nullability: Nullability, settings: ConverterSettings)
  : MayBeNullableType(nullability, settings) {

    override fun generateCode(builder: CodeBuilder) {
        if (elementType is PrimitiveType) {
            builder append elementType append "Array" append isNullableStr
        }
        else {
            builder append "Array<" append elementType append ">" append isNullableStr
        }
    }

    override fun toNotNullType(): Type = ArrayType(elementType, Nullability.NotNull, settings).assignPrototypesFrom(this)
    override fun toNullableType(): Type = ArrayType(elementType, Nullability.Nullable, settings).assignPrototypesFrom(this)
}

class InProjectionType(val bound: Type) : NotNullType() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "in " append bound
    }
}

class OutProjectionType(val bound: Type) : NotNullType() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "out " append bound
    }
}

class StarProjectionType : NotNullType() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("*")
    }
}

class PrimitiveType(val name: Identifier) : NotNullType() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(name)
    }
}

class VarArgType(val type: Type) : NotNullType() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(type)
    }
}
