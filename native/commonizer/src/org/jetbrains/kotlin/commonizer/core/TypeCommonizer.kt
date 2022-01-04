/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.utils.safeCastValues

class TypeCommonizer(
    private val classifiers: CirKnownClassifiers,
    val options: Options = Options.default
) : NullableSingleInvocationCommonizer<CirType> {

    private val classOrTypeAliasTypeCommonizer = ClassOrTypeAliasTypeCommonizer(this, classifiers)
    private val flexibleTypeCommonizer = FlexibleTypeAssociativeCommonizer(this)

    override fun invoke(values: List<CirType>): CirType? {
        values.safeCastValues<CirType, CirClassOrTypeAliasType>()?.let { types ->
            return classOrTypeAliasTypeCommonizer(types)
        }

        values.safeCastValues<CirType, CirTypeParameterType>()?.let { types ->
            return TypeParameterTypeCommonizer.commonize(types)
        }

        values.safeCastValues<CirType, CirFlexibleType>()?.let { types ->
            return flexibleTypeCommonizer(types)
        }

        return null
    }

    data class Options(
        val enableOptimisticNumberTypeCommonization: Boolean = true,
        val enableCovariantNullabilityCommonization: Boolean = false,
        val enableForwardTypeAliasSubstitution: Boolean = true,
        val enableBackwardsTypeAliasSubstitution: Boolean = true,
    ) {

        fun withOptimisticNumberTypeCommonizationEnabled(enabled: Boolean = true): Options {
            return if (enableOptimisticNumberTypeCommonization == enabled) this
            else copy(enableOptimisticNumberTypeCommonization = enabled)
        }

        fun withCovariantNullabilityCommonizationEnabled(enabled: Boolean = true): Options {
            return if (enableCovariantNullabilityCommonization == enabled) this
            else copy(enableCovariantNullabilityCommonization = enabled)
        }

        fun withForwardTypeAliasSubstitutionEnabled(enabled: Boolean = true): Options {
            return if (enableForwardTypeAliasSubstitution == enabled) this
            else copy(enableForwardTypeAliasSubstitution = enabled)
        }

        fun withBackwardsTypeAliasSubstitutionEnabled(enabled: Boolean = true): Options {
            return if (enableBackwardsTypeAliasSubstitution == enabled) this
            else copy(enableBackwardsTypeAliasSubstitution = enabled)
        }

        fun withTypeAliasSubstitutionEnabled(enabled: Boolean = true): Options {
            return withForwardTypeAliasSubstitutionEnabled(enabled).withBackwardsTypeAliasSubstitutionEnabled(enabled)
        }

        companion object {
            val default = Options()
        }
    }

    fun withOptions(options: Options): TypeCommonizer {
        return if (this.options == options) this
        else TypeCommonizer(classifiers, options)
    }

    inline fun withOptions(createNewOptions: Options.() -> Options): TypeCommonizer {
        return withOptions(options.createNewOptions())
    }
}

private object TypeParameterTypeCommonizer : AssociativeCommonizer<CirTypeParameterType> {
    override fun commonize(first: CirTypeParameterType, second: CirTypeParameterType): CirTypeParameterType? {
        // Real type parameter commonization is performed in TypeParameterCommonizer.
        // Here it is enough to check that type parameter indices and nullability are equal.
        if (first == second) return first
        return null
    }
}

private class FlexibleTypeAssociativeCommonizer(
    private val typeCommonizer: TypeCommonizer
) : NullableSingleInvocationCommonizer<CirFlexibleType> {
    override fun invoke(values: List<CirFlexibleType>): CirFlexibleType? {
        val lowerBound = typeCommonizer(values.map { it.lowerBound }) ?: return null
        val upperBound = typeCommonizer(values.map { it.upperBound }) ?: return null
        return CirFlexibleType(
            lowerBound = lowerBound as CirSimpleType,
            upperBound = upperBound as CirSimpleType
        )
    }
}
