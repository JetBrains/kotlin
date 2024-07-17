/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.domains.Injection
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.callables.CallableSignatureData
import org.jetbrains.kotlin.formver.names.NameMatcher
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.names.SimpleKotlinName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * Represents our representation of a Kotlin type.
 *
 * Due to name mangling, the mapping between Kotlin types and TypeEmbeddings must be 1:1.
 *
 * All type embeddings must be `data` classes or objects!
 */
interface TypeEmbedding : TypeInvariantHolder {
    /**
     * A Viper expression with the runtime representation of the type.
     *
     * The Viper values are defined in RuntimeTypeDomain and are used for casting, subtyping and the `is` operator.
     */
    val runtimeType: Exp

    /**
     * Name representing the type, used for distinguishing overloads.
     *
     * It may at some point necessary to make a `TypeName` hierarchy of some sort to
     * represent these names, but we do it inline for now.
     */
    val name: MangledName

    /**
     * Perform an action on every field and collect the results.
     *
     * Note that for fake fields that are taken from interfaces, this may visit some fields twice.
     * Use `flatMapUniqueFields` if you want to avoid that.
     */
    fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> = listOf()

    /**
     * Get a nullable version of this type embedding.
     *
     * Note that nullability doesn't stack, hence nullable types must return themselves.
     */
    fun getNullable(): NullableTypeEmbedding = NullableTypeEmbedding(this)

    /**
     * Drop nullability, if it is present.
     */
    fun getNonNullable(): TypeEmbedding = this

    val isNullable: Boolean
        get() = false
}

data object UnitTypeEmbedding : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.unitType()
    override val name = object : MangledName {
        override val mangled: String = "T_Unit"
    }
}

data object NothingTypeEmbedding : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.nothingType()
    override val name = object : MangledName {
        override val mangled: String = "T_Nothing"
    }

    override fun pureInvariants(): List<TypeInvariantEmbedding> = listOf(FalseTypeInvariant)
}

data object AnyTypeEmbedding : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.anyType()
    override val name = object : MangledName {
        override val mangled: String = "T_Any"
    }
}

data object NullableAnyTypeEmbedding : TypeEmbedding by NullableTypeEmbedding(AnyTypeEmbedding)

data object IntTypeEmbedding : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.intType()
    override val name = object : MangledName {
        override val mangled: String = "T_Int"
    }
}

data object BooleanTypeEmbedding : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.boolType()
    override val name = object : MangledName {
        override val mangled: String = "T_Boolean"
    }
}


data class NullableTypeEmbedding(val elementType: TypeEmbedding) : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.nullable(elementType.runtimeType)
    override val name = object : MangledName {
        override val mangled: String = "N" + elementType.name.mangled
    }

    override fun accessInvariants(): List<TypeInvariantEmbedding> = elementType.accessInvariants().map { IfNonNullInvariant(it) }
    override fun pureInvariants(): List<TypeInvariantEmbedding> = elementType.pureInvariants().map { IfNonNullInvariant(it) }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant(): TypeInvariantEmbedding? =
        elementType.sharedPredicateAccessInvariant()?.let { IfNonNullInvariant(it) }

    override fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding? =
        elementType.uniquePredicateAccessInvariant()?.let { IfNonNullInvariant(it) }

    override fun getNullable(): NullableTypeEmbedding = this
    override fun getNonNullable(): TypeEmbedding = elementType

    override val isNullable = true
}

data class FunctionTypeEmbedding(val signature: CallableSignatureData) : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.functionType()
    override val name = object : MangledName {
        override val mangled: String =
            "fun_take\$${signature.formalArgTypes.joinToString("$") { it.name.mangled }}\$return\$${signature.returnType.name.mangled}"
    }
}

data class ClassTypeEmbedding(val className: ScopedKotlinName) : TypeEmbedding {
    private var _details: ClassEmbeddingDetails? = null
    val details: ClassEmbeddingDetails
        get() = _details ?: error("Details of $className have not been initialised yet.")

    fun initDetails(details: ClassEmbeddingDetails) {
        require(_details == null) { "Class details already initialized" }
        _details = details
    }

    val hasDetails: Boolean
        get() = _details != null

    // TODO: incorporate generic parameters.
    override val name = object : MangledName {
        override val mangled: String = "T_class_${className.mangled}"
    }

    val runtimeTypeFunc = RuntimeTypeDomain.classTypeFunc(name)
    override val runtimeType: Exp = runtimeTypeFunc()

    override fun accessInvariants(): List<TypeInvariantEmbedding> = details.accessInvariants()

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant() = details.sharedPredicateAccessInvariant()

    override fun uniquePredicateAccessInvariant() = details.uniquePredicateAccessInvariant()
}


inline fun TypeEmbedding.injectionOr(default: () -> Injection): Injection = when (this) {
    IntTypeEmbedding -> RuntimeTypeDomain.intInjection
    BooleanTypeEmbedding -> RuntimeTypeDomain.boolInjection
    else -> default()
}

private fun TypeEmbedding.isCollectionTypeNamed(name: String): Boolean =
    if (this !is ClassTypeEmbedding) false
    else NameMatcher.matchGlobalScope(this.className) {
        ifInCollectionsPkg {
            ifClassName(name) {
                return true
            }
        }
        return false
    }

fun TypeEmbedding.isInheritorOfCollectionTypeNamed(name: String): Boolean =
    if (this !is ClassTypeEmbedding) false else isCollectionTypeNamed(name) || details.superTypes.any {
        it.isInheritorOfCollectionTypeNamed(name)
    }

val TypeEmbedding.isCollectionInheritor
    get() = isInheritorOfCollectionTypeNamed("Collection")

fun TypeEmbedding.subTypeInvariant() = SubTypeInvariantEmbedding(this)

