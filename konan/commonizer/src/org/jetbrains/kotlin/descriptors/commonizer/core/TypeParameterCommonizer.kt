/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance

interface TypeParameterCommonizer : Commonizer<TypeParameterDescriptor, TypeParameter> {
    companion object {
        fun default(): TypeParameterCommonizer = DefaultTypeParameterCommonizer()
    }
}

private class DefaultTypeParameterCommonizer : TypeParameterCommonizer {
    private enum class State {
        EMPTY,
        ERROR,
        IN_PROGRESS
    }

    private var state = State.EMPTY
    private lateinit var name: Name
    private var isReified = false
    private lateinit var variance: Variance
    private val upperBounds = TypeParameterUpperBoundsCommonizer()

    override val result: TypeParameter
        get() = when (state) {
            State.EMPTY, State.ERROR -> throw IllegalCommonizerStateException()
            State.IN_PROGRESS -> CommonTypeParameter(
                name = name,
                isReified = isReified,
                variance = variance,
                upperBounds = upperBounds.result
            )
        }

    override fun commonizeWith(next: TypeParameterDescriptor): Boolean {
        state = when (state) {
            State.ERROR -> State.ERROR
            State.EMPTY -> {
                name = next.name
                isReified = next.isReified
                variance = next.variance

                if (!upperBounds.commonizeWith(next.upperBounds)) State.ERROR else State.IN_PROGRESS
            }
            State.IN_PROGRESS -> {
                if (isReified != next.isReified
                    || variance != next.variance
                    || !upperBounds.commonizeWith(next.upperBounds)
                ) State.ERROR else State.IN_PROGRESS
            }
        }

        return state != State.ERROR
    }
}

private class TypeParameterUpperBoundsCommonizer : AbstractListCommonizer<KotlinType, UnwrappedType>(
    singleElementCommonizerFactory = { TypeCommonizer.default() }
)

interface TypeParameterListCommonizer : Commonizer<List<TypeParameterDescriptor>, List<TypeParameter>> {
    companion object {
        fun default(): TypeParameterListCommonizer = DefaultTypeParameterListCommonizer()
    }
}

private class DefaultTypeParameterListCommonizer :
    TypeParameterListCommonizer,
    AbstractNamedListCommonizer<TypeParameterDescriptor, TypeParameter>(
        singleElementCommonizerFactory = { TypeParameterCommonizer.default() }
    )
