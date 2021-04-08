/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.torch

fun Float.toRoundedString(digits: Int = 0): String {
    var factor = 1

    for (i in 0 until digits) {
        factor *= 10
    }

    return (kotlin.math.round(this * factor) / factor).toString()
}

fun Float.toPercentageString(roundToDigits: Int = 1) = (this * 100).toRoundedString(roundToDigits)

fun List<Float>.maxIndex() = withIndex().maxByOrNull { it.value }!!.index

fun accuracy(predictionBatch: FloatMatrix, labelBatch: FloatMatrix): Float {
    val resultIndexes = predictionBatch.toList().map { it.maxIndex() }
    val labelBatchIndexes = labelBatch.toList().map { it.maxIndex() }
    return resultIndexes.zip(labelBatchIndexes).
            count { (result, label) -> result == label }.toFloat() / resultIndexes.size
}

fun Backpropagatable<FloatMatrix, FloatMatrix>.trainClassifier(
        dataset: Dataset,
        lossByLabels: (FloatMatrix) -> Backpropagatable<FloatMatrix, FloatVector>,
        learningRateByProgress: (Float) -> Float = { 5f * kotlin.math.exp(-it * 3) },
        batchSize: Int = 64,
        iterations: Int = 500) {

    for (i in 0 until iterations) {
        disposeScoped {
            val (inputBatch, labelBatch) = dataset.sampleBatch(batchSize)
            val errorNetwork = this@trainClassifier before lossByLabels(labelBatch)
            val forwardResults = use { errorNetwork.forwardPass(inputBatch) }
            val accuracy = accuracy(forwardResults.hidden, labelBatch)
            val progress = i.toFloat() / iterations
            val learningRate = learningRateByProgress(progress)
            val backpropResults = use { forwardResults.backpropagate(outputGradient = tensor(learningRate)) }
            val crossEntropy = forwardResults.output[0]
            backpropResults.descend()
            println("Iteration ${i + 1}/$iterations: " +
                    "${accuracy.toPercentageString()}% training batch accuracy, " +
                    "cross entropy loss = ${crossEntropy.toRoundedString(4)}, " +
                    "learning rate = ${learningRate.toRoundedString(4)}")
        }
    }
}

fun Backpropagatable<FloatMatrix, FloatMatrix>.testClassifier(dataset: Dataset, batchSize: Int = 100): Float {
    val testBatches = dataset.testBatches(batchSize)
    return testBatches.withIndex().map { (i, batchPair) ->
        val (inputBatch, outputBatch) = batchPair
        val accuracy = accuracy(this.forwardPass(inputBatch).output, outputBatch)
        println("Test batch ${i + 1}/${testBatches.size}: ${accuracy.toPercentageString()}% accuracy")
        accuracy * inputBatch.shape[0]
    }.sum() / dataset.inputs.size
}

fun randomInit(size: Int) = random(-.01, .01, size)
fun randomInit(size0: Int, size1: Int) = random(-.1, .1, size0, size1)

fun linear(inputSize: Int, outputSize: Int) = Linear(randomInit(outputSize, inputSize), randomInit(outputSize))
fun twoLayerClassifier(dataset: Dataset, hiddenSize: Int = 64) =
        linear(dataset.inputs[0].size, hiddenSize) before Relu before
                linear(hiddenSize, dataset.labels[0].size) before Softmax

fun main() {
    val trainingDataset = MNIST.labeledTrainingImages()
    val predictionNetwork = twoLayerClassifier(trainingDataset)
    predictionNetwork.trainClassifier(trainingDataset, lossByLabels = { CrossEntropyLoss(labels = it) })

    val testDataset = MNIST.labeledTestImages()
    val averageAccuracy = predictionNetwork.testClassifier(testDataset)
    println("Accuracy on the test set: ${averageAccuracy.toPercentageString()}")
}
