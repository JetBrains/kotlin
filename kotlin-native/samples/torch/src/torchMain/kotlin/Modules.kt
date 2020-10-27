/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.torch

import kotlinx.cinterop.*
import torch.*

// Defines network modules with the ability for backpropagation using both TH.h and THNN.h from the ATen library

abstract class Backpropagatable<Input : Disposable, Output : Disposable> {
    abstract inner class ForwardResults(val input: Input) : DisposableContainer() {
        init {
            use { input }
        }

        abstract val output: Output
        abstract fun backpropagate(outputGradient: Output): BackpropagationResults
    }

    abstract inner class BackpropagationResults(
            val input: Input,
            val output: Output,
            val outputGradient: Output
    ) : DisposableContainer() {
        init {
            use { input }
            use { output }
            use { outputGradient }
        }

        abstract val inputGradient: Input
        abstract fun descend()
    }

    abstract fun forwardPass(input: Input): ForwardResults
}

abstract class Module<Input : Disposable, Output : Disposable, Parameters> : Backpropagatable<Input, Output>() {
    abstract var parameters: Parameters
    abstract fun parametersToList(parameters: Parameters): List<FloatTensor>
    abstract fun parametersFromList(list: List<FloatTensor>): Parameters
    private val parameterList get() = parametersToList(parameters)

    abstract operator fun invoke(input: Input): Output
    abstract fun inputGradient(input: Input, outputGradient: Output, output: Output): Input
    abstract fun parameterGradient(input: Input, outputGradient: Output, inputGradient: Input): Parameters

    override fun forwardPass(input: Input) = object : ForwardResults(input) {
        override val output = use { this@Module(input) }
        override fun backpropagate(outputGradient: Output) =
                object : Backpropagatable<Input, Output>.BackpropagationResults(input, output, outputGradient) {
                    override val inputGradient = use { this@Module.inputGradient(input, outputGradient, output) }
                    val parameterGradient = this@Module.parameterGradient(input,
                            outputGradient = outputGradient, inputGradient = inputGradient)

                    override fun descend() = this@Module.descend(parameterGradient)
                }
    }

    open fun descend(parameterGradient: Parameters) {
        parameters = parametersFromList(parameterList.zip(
                parametersToList(parameterGradient)) { parameter, gradient -> parameter - gradient })
    }
}

abstract class ParameterFreeModule<Input : Disposable, Output : Disposable> : Module<Input, Output, Unit>() {
    override var parameters = Unit
    override fun parametersToList(parameters: Unit) = emptyList<FloatTensor>()
    override fun parametersFromList(list: List<FloatTensor>) = Unit
    override fun parameterGradient(input: Input, outputGradient: Output, inputGradient: Input) = Unit
    override fun descend(parameterGradient: Unit) {}
}

class Chain<Input : Disposable, Hidden : Disposable, Output : Disposable>(
        val module1: Backpropagatable<Input, Hidden>,
        val module2: Backpropagatable<Hidden, Output>
) : Backpropagatable<Input, Output>() {
    override fun forwardPass(input: Input) = ChainForwardResults(input)

    inner class ChainForwardResults(input: Input) : ForwardResults(input) {
        val result1 = use { module1.forwardPass(input) }
        val hidden = result1.output
        val result2 = use { module2.forwardPass(result1.output) }
        override val output = result2.output
        override fun backpropagate(outputGradient: Output) =
                object : Backpropagatable<Input, Output>.BackpropagationResults(input, output, outputGradient) {
                    val backpropResults2 = use { result2.backpropagate(outputGradient) }
                    val hiddenGradient = backpropResults2.inputGradient
                    val backpropResults1 = use { result1.backpropagate(hiddenGradient) }

                    override val inputGradient = backpropResults1.inputGradient

                    override fun descend() {
                        backpropResults1.descend()
                        backpropResults2.descend()
                    }
                }
    }

    override fun toString() = "$module1 before $module2"
}

infix fun <Input : Disposable, Hidden : Disposable, Output : Disposable> Backpropagatable<Input, Hidden>.before(
        other: Backpropagatable<Hidden, Output>) = Chain(this, other)

object Abs : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatAbs_updateOutput(cValuesOf<FloatVar>(), input.raw, it.raw)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) =
            initializedTensor(input.shape[0], input.shape[1]) {
                THNN_FloatAbs_updateGradInput(null, input.raw, outputGradient.raw, it.raw)
            }
}

object Relu : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatLeakyReLU_updateOutput(null, input.raw, it.raw, 0.0, false)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) =
            initializedTensor(input.shape[0], input.shape[1]) {
                THNN_FloatLeakyReLU_updateGradInput(null, input.raw, outputGradient.raw, it.raw, 0.0, false)
            }
}

object Softmax : ParameterFreeModule<FloatMatrix, FloatMatrix>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatSoftMax_updateOutput(null, input.raw, it.raw, 1)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) =
            initializedTensor(input.shape[0], input.shape[1]) {
                THNN_FloatSoftMax_updateGradInput(null, input.raw, outputGradient.raw, it.raw, output.raw, 1)
            }
}

class MeanSquaredError(val labels: FloatMatrix) : ParameterFreeModule<FloatMatrix, FloatVector>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(1) {
        THNN_FloatMSECriterion_updateOutput(null, input.raw, labels.raw, it.raw,
                sizeAverage = true, reduce = true)
    }

    override fun inputGradient(
            input: FloatMatrix,
            outputGradient: FloatVector,
            output: FloatVector
    ) = initializedTensor(input.shape[0], input.shape[1]) {
        THNN_FloatMSECriterion_updateGradInput(null, input.raw, labels.raw,
                outputGradient.raw, it.raw, sizeAverage = true, reduce = true)
    }
}

class CrossEntropyLoss(val labels: FloatMatrix) : ParameterFreeModule<FloatMatrix, FloatVector>() {
    override operator fun invoke(input: FloatMatrix) = initializedTensor(1) {
        THNN_FloatBCECriterion_updateOutput(null, input.raw, labels.raw, it.raw,
                sizeAverage = true, reduce = true, weights = null)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatVector, output: FloatVector) =
            initializedTensor(input.shape[0], input.shape[1]) {
                THNN_FloatBCECriterion_updateGradInput(null, input.raw, labels.raw, outputGradient.raw,
                        it.raw, sizeAverage = true, reduce = true, weights = null)
            }
}

data class Linear(
        var weight: FloatMatrix,
        var bias: FloatVector) : Module<FloatMatrix, FloatMatrix, Pair<FloatMatrix, FloatVector>>() {
    val inputSize = weight.shape[1]
    val outputSize = weight.shape[0]
    val addBuffer = uninitializedTensor(outputSize)

    override operator fun invoke(input: FloatMatrix) = initializedTensor(input.shape[0], outputSize) {
        THNN_FloatLinear_updateOutput(null, input.raw, it.raw, weight.raw, bias.raw, addBuffer.raw)
    }

    override fun inputGradient(input: FloatMatrix, outputGradient: FloatMatrix, output: FloatMatrix) =
            initializedTensor(input.shape[0], inputSize) {
                THNN_FloatLinear_updateGradInput(null, input.raw, outputGradient.raw, it.raw, weight.raw)
            }

    override fun parameterGradient(
            input: FloatMatrix,
            outputGradient: FloatMatrix,
            inputGradient: FloatMatrix
    ): Pair<FloatMatrix, FloatVector> {
        val biasGradient = zeros(outputSize)
        val weightGradient = zeros(weight.shape[0], weight.shape[1]).also {
            THNN_FloatLinear_accGradParameters(null, input.raw, outputGradient.raw, inputGradient.raw, weight.raw,
                    bias.raw, it.raw, biasGradient.raw, addBuffer.raw, 1.0)
        }

        return weightGradient to biasGradient
    }

    override var parameters: Pair<FloatMatrix, FloatVector>
        get() = weight to bias
        set(value) {
            weight = value.first
            bias = value.second
        }

    override fun parametersToList(parameters: Pair<FloatMatrix, FloatVector>) =
            listOf(parameters.first, parameters.second)

    override fun parametersFromList(list: List<FloatTensor>) = list.first().asMatrix() to list.last().asVector()
}