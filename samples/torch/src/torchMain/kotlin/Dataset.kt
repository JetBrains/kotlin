/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.torch

import kotlinx.cinterop.*
import platform.posix.*

data class Dataset(val inputs: List<FloatArray>, val labels: List<FloatArray>) {
    fun batch(indices: List<Int>): Pair<FloatMatrix, FloatMatrix> {
        val inputBatch = tensor(*(indices.map { inputs[it].toTypedArray() }.toTypedArray()))
        val labelBatch = tensor(*(indices.map { labels[it].toTypedArray() }.toTypedArray()))
        return inputBatch to labelBatch
    }

    fun sampleBatch(batchSize: Int) = batch((0 until batchSize).map { randomInt(inputs.size) })
    private fun batchAt(batchIndex: Int, batchSize: Int) =
            batch((0 until inputs.size).drop(batchSize + batchIndex).take(batchSize))

    fun testBatches(batchSize: Int) = (0 until inputs.size / batchSize).map { batchAt(it, batchSize = batchSize) }
}

/**
 * Provides the MNIST labeled handwritten digit dataset, described at http://yann.lecun.com/exdb/mnist/
 */
object MNIST {
    private fun readFileData(fileName: String) = memScoped {
        val path = "build/3rd-party/MNIST/$fileName"
        fun fail(): Nothing = throw Error("Cannot read input file $path")

        val size = alloc<stat>().also { if (stat(path, it.ptr) != 0) fail() }.st_size.toInt()

        println("Reading $size bytes from $path...")

        val file = fopen(path, "rb") ?: fail()
        try {
            ByteArray(size).also { fread(it.refTo(0), 1, size.convert(), file) }
        } finally {
            fclose(file)
        }
    }

    private fun Byte.reinterpretAsUnsigned() = this.toInt().let { it + if (it < 0) 256 else 0 }

    private fun unsignedBytesToInt(bytes: List<Byte>) =
            bytes.withIndex().map { (i, value) -> value.reinterpretAsUnsigned().shl(8 * (3 - i)) }.sum()

    private val intSize = 4
    private fun ByteArray.getIntAt(index: Int) =
            unsignedBytesToInt((index until (index + intSize)).map { this[it] })

    private val imageLength = 28
    private val imageSize = imageLength * imageLength

    private fun ByteArray.getImageAt(index: Int) =
            FloatArray(imageSize) { this[index + it].reinterpretAsUnsigned().toFloat() / 255 }

    private fun oneHot(size: Int, index: Int) = FloatArray(size) { if (it == index) 1f else 0f }

    private fun readLabels(fileName: String, totalLabels: Int = 10): List<FloatArray> {
        val data = readFileData(fileName)
        val check = data.getIntAt(0)
        val expectedCheck = 2049
        if (check != 2049) throw Error("File should start with int $expectedCheck, but was $check.")

        val count = data.getIntAt(4)

        val offset = 8

        if (count + offset != data.size) throw Error("Unexpected file size: ${data.size}.")

        return (0 until count).map { oneHot(totalLabels, index = data[offset + it].reinterpretAsUnsigned()) }
    }

    private fun readImages(fileName: String): List<FloatArray> {
        val data = readFileData(fileName)
        val check = data.getIntAt(0)
        val expectedCheck = 2051
        if (check != expectedCheck) throw Error("File should start with int $expectedCheck, but was $check.")

        val count = data.getIntAt(4)
        val width = data.getIntAt(8)
        val height = data.getIntAt(12)

        val offset = 16

        if (width != imageLength) throw Error()
        if (height != imageLength) throw Error()

        if (count * imageSize + offset != data.size) throw Error("Unexpected file size: ${data.size}.")

        return (0 until count).map { data.getImageAt(offset + imageSize * it) }
    }

    fun labeledTrainingImages() = Dataset(
            inputs = readImages("train-images-idx3-ubyte"),
            labels = readLabels("train-labels-idx1-ubyte"))

    fun labeledTestImages() = Dataset(
            inputs = readImages("t10k-images-idx3-ubyte"),
            labels = readLabels("t10k-labels-idx1-ubyte"))
}