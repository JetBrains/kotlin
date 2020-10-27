/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.torch

// If you are curious you can also try one of these

private fun demonstrateTensors() {
    disposeScoped {
        val x = use { tensor(0f, 1f, 2f) }
        val y = use { tensor(0f, -1f, -2f) }
        val m = use {
            tensor(
                    arrayOf(1f, -1f, 0f),
                    arrayOf(0f, -1f, 0f),
                    arrayOf(0f, 0f, -.5f))
        }

        println("Hello, Torch!\nx = $x\ny = $y\n" +
                "|x| = ${x.abs()}\n|y| = ${y.abs()}\n" +
                "2x=${use { x * 2f }}\nx+y = ${use { x + y }}\nx-y = ${use { x - y }}\nxy = ${x * y}\n" +
                "m=\n${use { m }}\nm·y = ${use { m * y }}\nm+m =\n${use { m + m }}\nm·m =\n${use { m * m }}")
    }
}

private fun demonstrateModules() {
    val input = tensor(arrayOf(-1f))
    val abs = Abs(input)
    println("abs of $input is $abs, gradient is ${Abs.inputGradient(input, tensor(arrayOf(1f)), abs)}")
    val relu = Relu(input)
    println("relu of $input is $relu, gradient is ${Relu.inputGradient(input, tensor(arrayOf(1f)), relu)}")
}

private fun demonstrateManualBackpropagationFor1LinearLayer(
        inputs: FloatMatrix = tensor(arrayOf(1f, -1f), arrayOf(1f, -1f)),
        labels: FloatMatrix = tensor(arrayOf(5f), arrayOf(5f)),
        learningRate: Float = .1f) {
    val linear = Linear(weight = randomInit(1, 2), bias = randomInit(1))
    val error = MeanSquaredError(labels)

    for (i in 0 until 100) {
        disposeScoped {
            val output = use { linear(inputs) }
            val loss = use { error(output) }
            val outputGradient = use { error.inputGradient(output, tensor(learningRate), loss) }
            val inputGradient = use { linear.inputGradient(inputs, outputGradient, output) }
            val parameterGradient = linear.parameterGradient(inputs, outputGradient, inputGradient).
                    also { use { it.first } }.also { use { it.second } }
            println("input: $inputs, \n" +
                    "output: $output, \n" +
                    "labels: $labels, \n" +
                    "mean squared error: $loss, \n" +
                    "output gradient: $outputGradient, \n" +
                    "input gradient: $inputGradient, \n" +
                    "parameter gradient: $parameterGradient")
            linear.weight -= parameterGradient.first
            linear.bias -= parameterGradient.second
        }
    }
}