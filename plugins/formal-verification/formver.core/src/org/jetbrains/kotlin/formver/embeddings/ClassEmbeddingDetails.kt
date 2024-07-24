/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.names.ClassScope
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.names.SimpleKotlinName
import org.jetbrains.kotlin.formver.names.classNameIfAny
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class ClassEmbeddingDetails(val type: ClassTypeEmbedding, val isInterface: Boolean) : TypeInvariantHolder {
    private var _superTypes: List<TypeEmbedding>? = null
    val superTypes: List<TypeEmbedding>
        get() = _superTypes ?: error("Super types of ${type.className} have not been initialised yet.")

    private val classSuperTypes: List<ClassTypeEmbedding>
        get() = superTypes.filterIsInstance<ClassTypeEmbedding>()

    fun initSuperTypes(newSuperTypes: List<TypeEmbedding>) {
        check(_superTypes == null) { "Super types of ${type.className} are already initialised." }
        _superTypes = newSuperTypes
    }

    private var _fields: Map<SimpleKotlinName, FieldEmbedding>? = null
    private var _sharedPredicate: Predicate? = null
    private var _uniquePredicate: Predicate? = null
    val fields: Map<SimpleKotlinName, FieldEmbedding>
        get() = _fields ?: error("Fields of ${type.className} have not been initialised yet.")
    val sharedPredicate: Predicate
        get() = _sharedPredicate ?: error("Predicate of ${type.className} has not been initialised yet.")
    val uniquePredicate: Predicate
        get() = _uniquePredicate ?: error("Unique Predicate of ${type.className} has not been initialised yet.")

    fun initFields(newFields: Map<SimpleKotlinName, FieldEmbedding>) {
        check(_fields == null) { "Fields of ${type.className} are already initialised." }
        _fields = newFields
        _sharedPredicate = ClassPredicateBuilder.build(this, type.name) {
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

    private val uniquePredicateName = object : MangledName {
        override val mangled: String = "Unique\$T_class_${type.className.mangled}"
    }

    /**
     * Find an embedding of a backing field by this name amongst the ancestors of this type.
     *
     * While in Kotlin only classes can have backing fields, and so searching interface supertypes is not strictly necessary,
     * due to the way we handle list size we need to search all types.
     */
    fun findField(name: SimpleKotlinName): FieldEmbedding? = fields[name]

    fun <R> flatMapFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> =
        classSuperTypes.flatMap { it.details.flatMapFields(action) } + fields.flatMap { (name, field) -> action(name, field) }

    // We can't easily implement this by recursion on the supertype structure since some supertypes may be seen multiple times.
    // TODO: figure out a nicer way to handle this.
    override fun accessInvariants(): List<TypeInvariantEmbedding> =
        flatMapUniqueFields { _, field -> field.accessInvariantsForParameter() }

    // Note: this function will replace accessInvariants when nested unfold will be implemented
    override fun sharedPredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(type.name, PermExp.WildcardPerm())

    override fun uniquePredicateAccessInvariant() =
        PredicateAccessTypeInvariantEmbedding(uniquePredicateName, PermExp.FullPerm())

    // Returns the sequence of classes in a hierarchy that need to be unfolded in order to access the given field
    fun hierarchyUnfoldPath(fieldName: ScopedKotlinName): Sequence<ClassTypeEmbedding> = sequence {
        val className = fieldName.scope.classNameIfAny
        require(className != null) { "Cannot find hierarchy unfold path of a field with no class scope" }
        if (className == type.className.name) {
            yield(this@ClassEmbeddingDetails.type)
        } else {
            val sup = superTypes.firstOrNull { it is ClassTypeEmbedding && !it.details.isInterface }
            if (sup is ClassTypeEmbedding) {
                yield(this@ClassEmbeddingDetails.type)
                yieldAll(sup.details.hierarchyUnfoldPath(fieldName))
            } else {
                throw IllegalArgumentException("Reached top of the hierarchy without finding the field")
            }
        }
    }

    fun <R> flatMapUniqueFields(action: (SimpleKotlinName, FieldEmbedding) -> List<R>): List<R> {
        val seenFields = mutableSetOf<SimpleKotlinName>()
        return flatMapFields { name, field ->
            seenFields.add(name).ifTrue {
                action(name, field)
            } ?: listOf()
        }
    }
}