/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.conversion.SpecialFields
import org.jetbrains.kotlin.formver.domains.AnyDomain
import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.embeddings.callables.CallableSignatureData
import org.jetbrains.kotlin.formver.embeddings.callables.FieldAccessFunction
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.formver.viper.ast.Type
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

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    fun predicateAccessInvariants(): List<TypeInvariantEmbedding> = emptyList()

    /**
     * A list of invariants that are already known to be true based on the Kotlin code being well-formed.
     *
     * We can provide these to Viper as assumptions rather than requiring them to be explicitly proven.
     * An example of this is when other systems (e.g. the type checker) have already proven these.
     *
     * TODO: This should probably always include the `subtypeInvariant`, but primitive types make that harder.
     * TODO: Can be included in the class predicate when unfolding works
     */
    fun provenInvariants(): List<TypeInvariantEmbedding> = emptyList()

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

    override fun pureInvariants(): List<TypeInvariantEmbedding> = listOf(FalseTypeInvariant)
}

data object AnyTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.anyType()
    override val viperType = AnyDomain.toType()
    override val name = object : MangledName {
        override val mangled: String = "T_Any"
    }

    override fun provenInvariants(): List<TypeInvariantEmbedding> = listOf(SubTypeInvariantEmbedding(this))
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

    // Due to initialisation order issues, this cannot be a simple `val` field.
    val nullVal: ExpEmbedding
        get() = NullLit(elementType)

    override fun provenInvariants(): List<TypeInvariantEmbedding> = listOf(SubTypeInvariantEmbedding(this))
    override fun accessInvariants(): List<TypeInvariantEmbedding> = elementType.accessInvariants().map { IfNonNullInvariant(it) }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun predicateAccessInvariants(): List<TypeInvariantEmbedding> =
        elementType.predicateAccessInvariants().map { IfNonNullInvariant(it) }

    override fun getNullable(): NullableTypeEmbedding = this
    override fun getNonNullable(): TypeEmbedding = elementType

    override val isNullable = true
}

abstract class UnspecifiedFunctionTypeEmbedding : TypeEmbedding {
    override val runtimeType = TypeDomain.functionType()
    override val viperType: Type = Type.Ref

    override fun provenInvariants(): List<TypeInvariantEmbedding> = listOf(SubTypeInvariantEmbedding(this))

    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        listOf(FieldAccessTypeInvariantEmbedding(SpecialFields.FunctionObjectCallCounterField, PermExp.FullPerm()))

    override fun dynamicInvariants(): List<TypeInvariantEmbedding> = listOf(CallCounterMonotonicTypeInvariantEmbedding)

    object CallCounterMonotonicTypeInvariantEmbedding : TypeInvariantEmbedding {
        override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
            LeCmp(
                Old(FieldAccess(exp, SpecialFields.FunctionObjectCallCounterField)),
                FieldAccess(exp, SpecialFields.FunctionObjectCallCounterField)
            )
    }
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

data class ClassTypeEmbedding(val className: ScopedKotlinName) : TypeEmbedding {
    private var _superTypes: List<TypeEmbedding>? = null
    val superTypes: List<TypeEmbedding>
        get() = _superTypes ?: throw IllegalStateException("Super types of $className have not been initialised yet.")

    private val classSuperTypes: List<ClassTypeEmbedding>
        get() = superTypes.filterIsInstance<ClassTypeEmbedding>()

    fun initSuperTypes(newSuperTypes: List<TypeEmbedding>) {
        if (_superTypes != null) throw IllegalStateException("Super types of $className are already initialised.")
        _superTypes = newSuperTypes
    }

    private var _fields: Map<SimpleKotlinName, FieldEmbedding>? = null
    private var _predicate: Predicate? = null
    val fields: Map<SimpleKotlinName, FieldEmbedding>
        get() = _fields ?: throw IllegalStateException("Fields of $className have not been initialised yet.")
    val predicate: Predicate
        get() = _predicate ?: throw IllegalStateException("Predicate of $className has not been initialised yet.")

    fun initFields(newFields: Map<SimpleKotlinName, FieldEmbedding>) {
        if (_fields != null) throw IllegalStateException("Fields of $className are already initialised.")
        _fields = newFields
        _predicate = initPredicate()
    }

    private fun initPredicate(): Predicate {
        val subjectEmbedding = VariableEmbedding(ClassPredicateSubjectName, this)
        val accessFields = fields.values
            .flatMap { it.accessInvariantsForParameter().fillHoles(subjectEmbedding) }

        // For the moment ALWAYS_WRITEABLE fields with some class type do not exist.
        // Whether to include them here will need to be considered in the future.
        // We need to pass in the field access since the predicates are simply the ones for the type.
        val accessFieldPredicates = fields.values
            .filter { it.accessPolicy == AccessPolicy.ALWAYS_READABLE }
            .flatMap { it.type.predicateAccessInvariants().fillHoles(FieldAccess(subjectEmbedding, it)) }

        val accessSuperTypesPredicates = superTypes
            .filterIsInstance<ClassTypeEmbedding>()
            .map { PredicateAccessTypeInvariantEmbedding(it.name).fillHole(subjectEmbedding) }

        val body = (accessFields + accessFieldPredicates + accessSuperTypesPredicates).toConjunction()
        return Predicate(name, listOf(subjectEmbedding.toLocalVarDecl()), body.pureToViper())
    }

    // Note: This is a preparation for upcoming pull requests, functions for predicates unfolding are just declared and not used.
    fun getterFunctions(): List<FieldAccessFunction> {
        val receiver = VariableEmbedding(GetterFunctionSubjectName, this)
        val getPropertyFunctions = fields.values
            .filter { field -> field.accessPolicy != AccessPolicy.ALWAYS_INHALE_EXHALE }
            .map { field -> FieldAccessFunction(name, field, FieldAccess(receiver, field).pureToViper()) }
        val getSuperPropertyFunctions = classSuperTypes.flatMap {
            it.flatMapUniqueFields { _, field ->
                if (field.accessPolicy != AccessPolicy.ALWAYS_INHALE_EXHALE) {
                    val unfoldingBody =
                        Exp.FuncApp(GetterFunctionName(it.name, field.name), listOf(receiver.toLocalVarUse()), field.type.viperType)
                    listOf(FieldAccessFunction(name, field, unfoldingBody))
                } else {
                    listOf()
                }
            }
        }
        return getPropertyFunctions + getSuperPropertyFunctions
    }

    override val runtimeType = TypeDomain.classType(className)
    override val viperType = Type.Ref

    // TODO: incorporate generic parameters.
    override val name = object : MangledName {
        override val mangled: String = "T_class_${className.mangled}"
    }

    override fun findField(name: SimpleKotlinName): FieldEmbedding? = fields[name] ?: findAncestorField(name)

    fun findAncestorField(name: SimpleKotlinName): FieldEmbedding? = superTypes.firstNotNullOfOrNull { it.findField(name) }

    override fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> =
        superTypes.flatMap { it.flatMapFields(action) } + fields.flatMap { (name, field) -> action(name, field) }

    // We can't easily implement this by recursion on the supertype structure since some supertypes may be seen multiple times.
    // TODO: figure out a nicer way to handle this.
    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        flatMapUniqueFields { _, field -> field.accessInvariantsForParameter() }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun predicateAccessInvariants(): List<TypeInvariantEmbedding> = listOf(PredicateAccessTypeInvariantEmbedding(name))

    override fun provenInvariants(): List<TypeInvariantEmbedding> = listOf(SubTypeInvariantEmbedding(this))
}