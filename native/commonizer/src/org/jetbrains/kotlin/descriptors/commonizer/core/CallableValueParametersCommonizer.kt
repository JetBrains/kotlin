/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import com.intellij.util.containers.FactoryMap
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirCallableMemberWithParameters
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasAnnotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.core.CallableValueParametersCommonizer.CallableToPatch.Companion.doNothing
import org.jetbrains.kotlin.descriptors.commonizer.core.CallableValueParametersCommonizer.CallableToPatch.Companion.patchCallables
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapIndexed
import org.jetbrains.kotlin.descriptors.commonizer.utils.isObjCInteropCallableAnnotation

class CallableValueParametersCommonizer(
    classifiers: CirKnownClassifiers
) : Commonizer<CirCallableMemberWithParameters, CallableValueParametersCommonizer.Result> {
    class Result(
        val hasStableParameterNames: Boolean,
        val valueParameters: List<CirValueParameter>,
        val patchCallables: () -> Unit
    )

    private class CallableToPatch(
        val callable: CirCallableMemberWithParameters,
        val originalNames: ValueParameterNames
    ) {
        init {
            check(originalNames is ValueParameterNames.Generated || originalNames is ValueParameterNames.Real)
        }

        val canNamesBeOverwritten by lazy { callable.canNamesBeOverwritten() }

        companion object {
            fun doNothing(): () -> Unit = {}

            fun List<CallableToPatch>.patchCallables(generated: Boolean, newNames: List<CirName>): () -> Unit {
                val callablesToPatch = filter { it.originalNames is ValueParameterNames.Generated == generated }
                    .takeIf { it.isNotEmpty() }
                    ?: return doNothing()

                return {
                    callablesToPatch.forEach { callableToPatch ->
                        val callable = callableToPatch.callable
                        callable.hasStableParameterNames = false
                        callable.valueParameters = callable.valueParameters.compactMapIndexed { index, valueParameter ->
                            val newName = newNames[index]
                            if (valueParameter.name != newName) {
                                CirValueParameter.createInterned(
                                    annotations = valueParameter.annotations,
                                    name = newName,
                                    returnType = valueParameter.returnType,
                                    varargElementType = valueParameter.varargElementType,
                                    declaresDefaultValue = valueParameter.declaresDefaultValue,
                                    isCrossinline = valueParameter.isCrossinline,
                                    isNoinline = valueParameter.isNoinline
                                )
                            } else valueParameter
                        }
                    }
                }
            }
        }
    }

    private sealed class ValueParameterNames {
        object Generated : ValueParameterNames()

        data class Real(val names: List<CirName>) : ValueParameterNames()

        class MultipleReal(valueParameters: List<CirValueParameter>) : ValueParameterNames() {
            val generatedNames: List<CirName> = generatedNames(valueParameters)
        }

        companion object {
            fun buildFor(callable: CirCallableMemberWithParameters): ValueParameterNames {
                val valueParameters = callable.valueParameters
                if (valueParameters.isEmpty())
                    return Real(emptyList())

                var real = false
                val names = callable.valueParameters.mapIndexed { index, valueParameter ->
                    val name = valueParameter.name
                    val plainName = name.name

                    if (valueParameter.varargElementType != null) {
                        if (plainName != VARIADIC_ARGUMENTS) {
                            real = true
                        }
                    } else {
                        if (!plainName.startsWith(REGULAR_ARGUMENT_PREFIX)
                            || index.toString() != plainName.substring(REGULAR_ARGUMENT_PREFIX.length)
                        ) {
                            real = true
                        }
                    }

                    name
                }

                return if (real) Real(names) else Generated
            }

            fun generatedNames(valueParameters: List<CirValueParameter>): List<CirName> =
                valueParameters.mapIndexed { index, valueParameter ->
                    if (valueParameter.varargElementType != null) {
                        VARIADIC_ARGUMENTS_NAME
                    } else {
                        REGULAR_ARGUMENT_NAMES.getValue(index)
                    }
                }
        }
    }

    private val valueParameters = ValueParameterListCommonizer(classifiers)
    private val callables: MutableList<CallableToPatch> = mutableListOf()
    private var hasStableParameterNames = true
    private var valueParameterNames: ValueParameterNames? = null
    private var error = false

    override val result: Result
        get() {
            // don't inline `patchCallables` property;
            // valueParameters.overwriteNames() should be called strongly before valueParameters.result
            val patchCallables = when (val valueParameterNames = checkState(valueParameterNames, error)) {
                ValueParameterNames.Generated -> doNothing()
                is ValueParameterNames.Real -> {
                    val newNames = valueParameterNames.names
                    valueParameters.overwriteNames(newNames)
                    callables.patchCallables(generated = true, newNames)
                }
                is ValueParameterNames.MultipleReal -> {
                    val generatedNames = valueParameterNames.generatedNames
                    valueParameters.overwriteNames(generatedNames)
                    callables.patchCallables(generated = false, generatedNames)
                }
            }

            return Result(
                hasStableParameterNames = hasStableParameterNames,
                valueParameters = valueParameters.result,
                patchCallables = patchCallables
            )
        }

    override fun commonizeWith(next: CirCallableMemberWithParameters): Boolean {
        if (error)
            return false

        error = !valueParameters.commonizeWith(next.valueParameters)
                || !commonizeValueParameterNames(next)

        return !error
    }

    private fun commonizeValueParameterNames(next: CirCallableMemberWithParameters): Boolean {
        val nextNames = ValueParameterNames.buildFor(next)
        val nextCallable = CallableToPatch(next, nextNames)

        valueParameterNames = when (val currentNames = valueParameterNames) {
            null -> {
                when (nextNames) {
                    ValueParameterNames.Generated,
                    is ValueParameterNames.Real -> {
                        hasStableParameterNames = next.hasStableParameterNames
                    }
                    else -> failIllegalState(currentNames, nextNames)
                }
                nextNames
            }
            ValueParameterNames.Generated -> {
                @Suppress("LiftReturnOrAssignment")
                when (nextNames) {
                    ValueParameterNames.Generated -> {
                        hasStableParameterNames = hasStableParameterNames && next.hasStableParameterNames
                    }
                    is ValueParameterNames.Real -> {
                        if (callables.any { !it.canNamesBeOverwritten }) return false
                        hasStableParameterNames = false
                    }
                    else -> failIllegalState(currentNames, nextNames)
                }
                nextNames
            }
            is ValueParameterNames.Real -> {
                when (nextNames) {
                    ValueParameterNames.Generated -> {
                        if (!nextCallable.canNamesBeOverwritten) return false
                        hasStableParameterNames = false
                        currentNames
                    }
                    is ValueParameterNames.Real -> {
                        if (nextNames == currentNames) {
                            hasStableParameterNames = hasStableParameterNames && next.hasStableParameterNames
                            currentNames
                        } else {
                            if (callables.any { !it.canNamesBeOverwritten } || !nextCallable.canNamesBeOverwritten) return false
                            hasStableParameterNames = false
                            ValueParameterNames.MultipleReal(nextCallable.callable.valueParameters)
                        }
                    }
                    else -> failIllegalState(currentNames, nextNames)
                }
            }
            is ValueParameterNames.MultipleReal -> {
                if (!nextCallable.canNamesBeOverwritten) return false
                currentNames
            }
        }

        callables += nextCallable

        return true
    }

    companion object {
        private const val VARIADIC_ARGUMENTS = "variadicArguments"
        private const val REGULAR_ARGUMENT_PREFIX = "arg"

        private val VARIADIC_ARGUMENTS_NAME = CirName.create(VARIADIC_ARGUMENTS)
        private val REGULAR_ARGUMENT_NAMES = FactoryMap.create<Int, CirName> { index ->
            CirName.create(REGULAR_ARGUMENT_PREFIX + index)
        }

        private fun CirCallableMemberWithParameters.canNamesBeOverwritten(): Boolean {
            return (this as CirHasAnnotations).annotations.none { it.type.classifierId.isObjCInteropCallableAnnotation }
        }

        private fun failIllegalState(current: ValueParameterNames?, next: ValueParameterNames): Nothing =
            throw IllegalCommonizerStateException("unexpected next state $next with current state $current")
    }
}
