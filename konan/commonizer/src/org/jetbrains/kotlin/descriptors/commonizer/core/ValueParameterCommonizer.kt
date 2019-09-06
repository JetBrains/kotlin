/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.isNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType

interface ValueParameterCommonizer : Commonizer<ValueParameterDescriptor, ValueParameter> {
    companion object {
        fun default(): ValueParameterCommonizer = DefaultValueParameterCommonizer()
    }
}

private class DefaultValueParameterCommonizer : ValueParameterCommonizer {
    private enum class State {
        EMPTY,
        ERROR,
        IN_PROGRESS
    }

    private var name: Name? = null
    private val returnType = TypeCommonizer.default()
    private var varargElementType: UnwrappedType? = null
    private var isCrossinline = true
    private var isNoinline = true

    private var state = State.EMPTY

    override val result: ValueParameter
        get() = when (state) {
            State.EMPTY, State.ERROR -> error("Can't commonize value parameter")
            State.IN_PROGRESS -> CommonValueParameter(
                name = name!!,
                returnType = returnType.result,
                varargElementType = varargElementType,
                isCrossinline = isCrossinline,
                isNoinline = isNoinline
            )
        }

    override fun commonizeWith(next: ValueParameterDescriptor): Boolean {
        if (state == State.ERROR)
            return true

        val result = !next.declaresDefaultValue() && returnType.commonizeWith(next.type)
        state = if (!result)
            State.ERROR
        else when {
            state == State.EMPTY -> {
                name = next.name
                varargElementType = next.varargElementType?.unwrap()
                isCrossinline = next.isCrossinline
                isNoinline = next.isNoinline

                State.IN_PROGRESS
            }
            varargElementType.isNull() != next.varargElementType.isNull() -> State.ERROR
            else -> {
                isCrossinline = isCrossinline && next.isCrossinline
                isNoinline = isNoinline && next.isNoinline

                State.IN_PROGRESS
            }
        }

        return state != State.ERROR
    }
}

interface ValueParameterListCommonizer : Commonizer<List<ValueParameterDescriptor>, List<ValueParameter>> {
    companion object {
        fun default(): ValueParameterListCommonizer = DefaultValueParameterListCommonizer()
    }
}

private class DefaultValueParameterListCommonizer :
    ValueParameterListCommonizer,
    NamedListWrappedCommonizer<ValueParameterDescriptor, ValueParameter>(
        subject = "value parameters",
        wrappedCommonizerFactory = { ValueParameterCommonizer.default() }
    )
