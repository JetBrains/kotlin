/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.klib

sealed interface DeclarationId
sealed interface ClassifierId

/**
 * Represents a class or a type alias.
 *
 * @param qualifiedName the fully qualified name. Examples: `Outer`, `Outer.Nested`, `org/sample/Outer.Nested`.
 */
data class ClassOrTypeAliasId(val qualifiedName: String) : DeclarationId, ClassifierId, Comparable<ClassOrTypeAliasId> {
    override fun compareTo(other: ClassOrTypeAliasId) = qualifiedName.compareTo(other.qualifiedName)
}

/**
 * Represents a class constructor.
 *
 * @param qualifiedName the fully qualified name. Examples: `Outer.<init>`, `Outer.Nested.<init>`, `org/sample/Outer.Nested.<init>`.
 * @param parameters value parameters.
 */
data class ConstructorId(val qualifiedName: String, val parameters: List<ParameterId>) : DeclarationId, Comparable<ConstructorId> {
    override fun compareTo(other: ConstructorId) = COMPARATOR.compare(this, other)

    companion object {
        private val COMPARATOR = compareBy(ConstructorId::qualifiedName)
            .thenByList(ConstructorId::parameters)
    }
}

/**
 * Represents a function or a property accessor.
 *
 * @param qualifiedName the fully qualified name. Examples: `foo`, `org/sample/foo`, `Outer.foo`, `Outer.Nested.foo`,
 *   `org/sample/Outer.Nested.foo`, `<get-bar>`, `org/sample/<get-bar>`, `Outer.<get-bar>`, `Outer.Nested.<get-bar>`,
 *   `org/sample/Outer.Nested.<get-bar>`.
 * @param contextReceivers context receivers.
 * @param extensionReceiver extension receiver.
 * @param parameters value parameters.
 */
data class FunctionId(
    val qualifiedName: String,
    val contextReceivers: List<TypeId>,
    val extensionReceiver: TypeId?,
    val parameters: List<ParameterId>,
    val returnType: TypeId,
) : DeclarationId, Comparable<FunctionId> {
    override fun compareTo(other: FunctionId) = COMPARATOR.compare(this, other)

    companion object {
        private val COMPARATOR = compareBy(FunctionId::qualifiedName)
            .thenByList(FunctionId::contextReceivers)
            .thenBy(FunctionId::extensionReceiver)
            .thenByList(FunctionId::parameters)
            .thenBy(FunctionId::returnType)
    }
}

/**
 * Represents a property.
 *
 * @param qualifiedName the fully qualified name. Examples: `foo`, `org/sample/foo`, `Outer.foo`, `Outer.Nested.foo`,
 *   `org/sample/Outer.Nested.foo`.
 * @param contextReceivers context receivers.
 * @param extensionReceiver extension receiver.
 */
data class PropertyId(
    val qualifiedName: String,
    val contextReceivers: List<TypeId>,
    val extensionReceiver: TypeId?,
    val returnType: TypeId,
) : DeclarationId, Comparable<PropertyId> {
    override fun compareTo(other: PropertyId) = COMPARATOR.compare(this, other)

    companion object {
        private val COMPARATOR = compareBy(PropertyId::qualifiedName)
            .thenByList(PropertyId::contextReceivers)
            .thenBy(PropertyId::extensionReceiver)
            .thenBy(PropertyId::returnType)
    }
}

/**
 * Represents a value parameter.
 *
 * @param name value parameter name.
 * @param type value parameter type.
 * @param isVararg if it is a vararg.
 */
data class ParameterId(val name: String, val type: TypeId, val isVararg: Boolean) : Comparable<ParameterId> {
    constructor(type: TypeId, isVararg: Boolean) : this(IGNORED_NAME, type, isVararg)

    override fun compareTo(other: ParameterId) = COMPARATOR.compare(this, other)

    companion object {
        const val IGNORED_NAME = "?"

        private val COMPARATOR = compareBy(
            ParameterId::type,
            ParameterId::isVararg,
            ParameterId::name,
        )
    }
}

/**
 * Represents a type parameter.
 *
 * @param index the type parameter index.
 */
data class TypeParameterId(val index: Int) : ClassifierId, Comparable<TypeParameterId> {
    override fun compareTo(other: TypeParameterId) = index.compareTo(other.index)
}

/**
 * Represents a type.
 *
 * @param classifier the classifier.
 * @param arguments type arguments.
 */
data class TypeId(val classifier: ClassifierId, val arguments: List<TypeArgumentId>) : Comparable<TypeId> {
    override fun compareTo(other: TypeId) = COMPARATOR.compare(this, other)

    companion object {
        val UNIT = TypeId(ClassOrTypeAliasId("kotlin/Unit"), emptyList())

        private val COMPARATOR = compareBy<TypeId>(
            { it.classifier as? ClassOrTypeAliasId },
            { it.classifier as? TypeParameterId }
        ).thenByList(TypeId::arguments)
    }
}

/**
 * Represents a type argument.
 */
sealed class TypeArgumentId : Comparable<TypeArgumentId> {
    final override fun compareTo(other: TypeArgumentId) = COMPARATOR.compare(this, other)

    data object Star : TypeArgumentId()
    data class Regular(val type: TypeId, val variance: VarianceId) : TypeArgumentId()

    enum class VarianceId { INVARIANT, IN, OUT }

    companion object {
        private val COMPARATOR = Comparator<TypeArgumentId> { left, right ->
            when {
                left is Regular && right is Regular -> REGULAR_ARGUMENT_COMPARATOR.compare(left, right)
                left is Regular -> 1
                right is Regular -> -1
                else -> 0
            }
        }

        private val REGULAR_ARGUMENT_COMPARATOR = compareBy(Regular::type, Regular::variance)
    }
}

private inline fun <T, R : Comparable<R>> Comparator<T>.thenByList(
    crossinline selector: (T) -> List<R>,
): Comparator<T> = Comparator { left, right ->
    compare(left, right).let { if (it != 0) return@Comparator it }

    val leftList = selector(left)
    val rightList = selector(right)

    leftList.size.compareTo(rightList.size).let { if (it != 0) return@Comparator it }

    for (index in leftList.indices) {
        leftList[index].compareTo(rightList[index]).let { if (it != 0) return@Comparator it }
    }

    0
}
