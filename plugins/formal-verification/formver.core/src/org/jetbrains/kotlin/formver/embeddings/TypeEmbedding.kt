/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.SpecialFields
import org.jetbrains.kotlin.formver.conversion.SpecialName
import org.jetbrains.kotlin.formver.domains.*
import org.jetbrains.kotlin.formver.embeddings.callables.CallableSignatureData
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Type

/**
 * Represents our representation of a Kotlin type.
 *
 * Due to name mangling, the mapping between Kotlin types and TypeEmbeddings must be 1:1.
 *
 * All type embeddings must be `data` classes or objects!
 */
interface TypeEmbedding {
    /**
     * A Viper expression with the runtime representation of the type.
     *
     * The Viper values are defined in TypingDomain and are used for casting, subtyping and the `is` operator.
     */
    val runtimeType: Exp

    /**
     * The Viper type that is used to represent objects of this type in the Viper result.
     */
    val viperType: Type

    /**
     * Name representing the type, used for distinguishing overloads.
     *
     * It may at some point necessary to make a `TypeName` hierarchy of some sort to
     * represent these names, but we do it inline for now.
     */
    val name: MangledName

    fun accessInvariants(v: Exp): List<Exp> = emptyList()

    /**
     * A list of invariants that are already known to be true based on the Kotlin code being well-formed.
     *
     * We can provide these to Viper as assumptions rather than requiring them to be explicitly proven.
     * An example of this is when other systems (e.g. the type checker) have already proven these.
     *
     * TODO: This should probably always include the `subtypeInvariant`, but primitive types make that harder.
     */
    fun provenInvariants(v: Exp): List<Exp> = emptyList()

    fun invariants(v: Exp): List<Exp> = emptyList()

    /**
     * Invariants that should correlate the old and new value of a value of this type
     * over a function call. When a caller gives away permissions to the callee, these
     * dynamic invariants give properties about the modifications of the argument as
     * part of the functions post conditions.
     * This is exclusively necessary for CallsInPlace.
     */
    fun dynamicInvariants(v: Exp): List<Exp> = emptyList()
}

/**
 * Invariant: the runtime type of an object is always a subtype of its compile-time type.
 */
fun TypeEmbedding.subtypeInvariant(v: Exp) = TypeDomain.isSubtype(TypeOfDomain.typeOf(v), runtimeType)

data object UnitTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.unitType()
    override val viperType: Type = UnitDomain.toType()
    override val name = object : MangledName {
        override val mangled: String = "T_Unit"
    }
}

data object NothingTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.nothingType()
    override val viperType: Type = UnitDomain.toType()
    override val name = object : MangledName {
        override val mangled: String = "T_Nothing"
    }

    override fun invariants(v: Exp): List<Exp> = listOf(Exp.BoolLit(false))
}

data object AnyTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.anyType()
    override val viperType = AnyDomain.toType()
    override val name = object : MangledName {
        override val mangled: String = "T_Any"
    }

    override fun provenInvariants(v: Exp) = listOf(subtypeInvariant(v))
}

data object IntTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.intType()
    override val viperType: Type = Type.Int
    override val name = object : MangledName {
        override val mangled: String = "T_Int"
    }
}

data object BooleanTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.booleanType()
    override val viperType: Type = Type.Bool
    override val name = object : MangledName {
        override val mangled: String = "T_Boolean"
    }
}

data class NullableTypeEmbedding(val elementType: TypeEmbedding) : TypeEmbedding {
    override val runtimeType = TypeDomain.nullableType(elementType.runtimeType)
    override val viperType: Type = NullableDomain.nullableType(elementType.viperType)
    override val name = object : MangledName {
        override val mangled: String = "N" + elementType.name.mangled
    }

    val nullVal: ExpEmbedding
        get() = NullLit(elementType)

    override fun provenInvariants(v: Exp) = listOf(subtypeInvariant(v))
}

abstract class UnspecifiedFunctionTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.functionType()
    override val viperType: Type = Type.Ref

    override fun provenInvariants(v: Exp): List<Exp> = listOf(subtypeInvariant(v))

    override fun accessInvariants(v: Exp) = listOf(v.fieldAccessPredicate(SpecialFields.FunctionObjectCallCounterField, PermExp.FullPerm()))

    override fun dynamicInvariants(v: Exp): List<Exp> =
        listOf(
            Exp.LeCmp(
                Exp.Old(v.fieldAccess(SpecialFields.FunctionObjectCallCounterField)),
                v.fieldAccess(SpecialFields.FunctionObjectCallCounterField)
            )
        )
}

/**
 * Some of our older code requires specific type annotations for built-ins with function types.
 * However, we don't actually want to distinguish these builtins by type, so we introduce this
 * type embedding as a workaround.
 */
data object LegacyUnspecifiedFunctionTypeEmbedding : UnspecifiedFunctionTypeEmbedding() {
    override val name: MangledName = SpecialName("legacy_function_object_type")
}

data class FunctionTypeEmbedding(val signature: CallableSignatureData) : UnspecifiedFunctionTypeEmbedding() {
    override val name = object : MangledName {
        override val mangled: String =
            "fun_take\$${signature.formalArgTypes.joinToString("$") { it.name.mangled }}\$return\$${signature.returnType.name.mangled}"
    }
}

data class ClassTypeEmbedding(val className: ScopedKotlinName, val superTypes: List<TypeEmbedding>) : TypeEmbedding {
    override val runtimeType = TypeDomain.classType(className)
    override val viperType = Type.Ref

    // TODO: incorporate generic parameters.
    override val name = object : MangledName {
        override val mangled: String = "T_class_${className.mangled}"
    }

    override fun provenInvariants(v: Exp) = listOf(subtypeInvariant(v))
}