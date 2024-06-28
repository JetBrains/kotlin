/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.domains.Injection
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.callables.CallableSignatureData
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

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
     * Find an embedding of a backing field by this name amongst the ancestors of this type.
     *
     * While in Kotlin only classes can have backing fields, and so searching interface supertypes is not strictly necessary,
     * due to the way we handle list size we need to search all types.
     *
     * Non-class types have no fields and so almost no types will implement this function.
     */
    fun findField(name: SimpleKotlinName): FieldEmbedding? = null

    /**
     * Perform an action on every field and collect the results.
     *
     * Note that for fake fields that are taken from interfaces, this may visit some fields twice.
     * Use `flatMapUniqueFields` if you want to avoid that.
     */
    fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> = listOf()

    /**
     * Invariants that provide access to a resource and thus behave linearly.
     */
    fun accessInvariants(): List<TypeInvariantEmbedding> = emptyList()

    // Note: these functions will replace accessInvariants when nested unfold will be implemented
    fun sharedPredicateAccessInvariant(): TypeInvariantEmbedding? = null
    fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding? = null

    /**
     * Invariants that do not depend on the heap, and so do not need to be repeated
     * once they have been established once.
     */
    fun pureInvariants(): List<TypeInvariantEmbedding> = emptyList()

    /**
     * Invariants that should correlate the old and new value of a value of this type
     * over a function call. When a caller gives away permissions to the callee, these
     * dynamic invariants give properties about the modifications of the argument as
     * part of the functions post conditions.
     * This is exclusively necessary for CallsInPlace.
     */
    fun dynamicInvariants(): List<TypeInvariantEmbedding> = emptyList()

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

fun <R> TypeEmbedding.flatMapUniqueFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> {
    val seenFields = mutableSetOf<SimpleKotlinName>()
    return flatMapFields { name, field ->
        seenFields.add(name).ifTrue {
            action(name, field)
        } ?: listOf()
    }
}

fun <R> TypeEmbedding.mapNotNullUniqueFields(action: (SimpleKotlinName, FieldEmbedding) -> R?): List<R> =
    flatMapUniqueFields { name, field ->
        action(name, field)?.let { listOf(it) } ?: emptyList()
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

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant(): TypeInvariantEmbedding? =
        elementType.sharedPredicateAccessInvariant()?.let { IfNonNullInvariant(it) }

    override fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding? =
        elementType.uniquePredicateAccessInvariant()?.let { IfNonNullInvariant(it) }

    override fun getNullable(): NullableTypeEmbedding = this
    override fun getNonNullable(): TypeEmbedding = elementType

    override val isNullable = true
}

abstract class UnspecifiedFunctionTypeEmbedding : TypeEmbedding {
    override val runtimeType = RuntimeTypeDomain.functionType()
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

data class ClassTypeEmbedding(val className: ScopedKotlinName, val isInterface: Boolean) : TypeEmbedding {
    private var _superTypes: List<TypeEmbedding>? = null
    val superTypes: List<TypeEmbedding>
        get() = _superTypes ?: error("Super types of $className have not been initialised yet.")

    private val classSuperTypes: List<ClassTypeEmbedding>
        get() = superTypes.filterIsInstance<ClassTypeEmbedding>()

    fun initSuperTypes(newSuperTypes: List<TypeEmbedding>) {
        check(_superTypes == null) { "Super types of $className are already initialised." }
        _superTypes = newSuperTypes
    }

    private var _fields: Map<SimpleKotlinName, FieldEmbedding>? = null
    private var _sharedPredicate: Predicate? = null
    private var _uniquePredicate: Predicate? = null
    val fields: Map<SimpleKotlinName, FieldEmbedding>
        get() = _fields ?: error("Fields of $className have not been initialised yet.")
    val sharedPredicate: Predicate
        get() = _sharedPredicate ?: error("Predicate of $className has not been initialised yet.")
    val uniquePredicate: Predicate
        get() = _uniquePredicate ?: error("Unique Predicate of $className has not been initialised yet.")

    fun initFields(newFields: Map<SimpleKotlinName, FieldEmbedding>) {
        check(_fields == null) { "Fields of $className are already initialised." }
        _fields = newFields
        _sharedPredicate = ClassPredicateBuilder.build(this, name) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                    forType {
                        addAccessToSharedPredicate()
                        includeSubTypeInvariants()
                    }
                }
            }
            forEachSuperType {
                addAccessToSharedPredicate()
            }
        }
        _uniquePredicate = ClassPredicateBuilder.build(this, uniquePredicateName) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                } else {
                    addAccessPermissions(PermExp.FullPerm())
                }
                forType {
                    addAccessToSharedPredicate()
                    if (isUnique) {
                        addAccessToUniquePredicate()
                    }
                    includeSubTypeInvariants()
                }
            }
            forEachSuperType {
                addAccessToUniquePredicate()
            }
        }
    }

    // TODO: incorporate generic parameters.
    override val name = object : MangledName {
        override val mangled: String = "T_class_${className.mangled}"
    }

    private val uniquePredicateName = object : MangledName {
        override val mangled: String = "Unique\$T_class_${className.mangled}"
    }

    val runtimeTypeFunc = RuntimeTypeDomain.classTypeFunc(name)
    override val runtimeType: Exp = runtimeTypeFunc()

    override fun findField(name: SimpleKotlinName): FieldEmbedding? = fields[name]

    override fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> =
        superTypes.flatMap { it.flatMapFields(action) } + fields.flatMap { (name, field) -> action(name, field) }

    // We can't easily implement this by recursion on the supertype structure since some supertypes may be seen multiple times.
    // TODO: figure out a nicer way to handle this.
    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        flatMapUniqueFields { _, field -> field.accessInvariantsForParameter() }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(name, PermExp.WildcardPerm())

    override fun uniquePredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

    // Returns the sequence of classes in a hierarchy that need to be unfolded in order to access the given field
    fun hierarchyUnfoldPath(fieldName: MangledName): Sequence<ClassTypeEmbedding> = sequence {
        if (fieldName is ScopedKotlinName && fieldName.scope is ClassScope) {
            if (fieldName.scope.className == className.name) {
                yield(this@ClassTypeEmbedding)
            } else {
                val sup = superTypes.firstOrNull { it is ClassTypeEmbedding && !it.isInterface }
                if (sup is ClassTypeEmbedding) {
                    yield(this@ClassTypeEmbedding)
                    yieldAll(sup.hierarchyUnfoldPath(fieldName))
                } else {
                    throw IllegalArgumentException("Reached top of the hierarchy without finding the field")
                }
            }
        } else {
            throw IllegalArgumentException("Cannot find hierarchy unfold path of a field with no class scope")
        }
    }
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
    if (this !is ClassTypeEmbedding) false else isCollectionTypeNamed(name) || superTypes.any {
        it.isInheritorOfCollectionTypeNamed(name)
    }

val TypeEmbedding.isCollectionInheritor
    get() = isInheritorOfCollectionTypeNamed("Collection")

fun TypeEmbedding.subTypeInvariant() = SubTypeInvariantEmbedding(this)

