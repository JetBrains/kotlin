/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.torch

import kotlinx.cinterop.*
import torch.*

// Defines tensor classes and operations using TH.h from the ATen library

abstract class FloatTensor(val raw: CPointer<THFloatTensor>) : Disposable {
    private val storage: CPointer<THFloatStorage> get() = raw.pointed.storage!!
    private val elements get() = storage.pointed
    private val data: CPointer<FloatVar> get() = elements.data!!
    private val size: CPointer<LongVar> get() = raw.pointed.size!!
    protected val nDimension: Int get() = raw.pointed.nDimension

    val shape: List<Int> get() = (0 until nDimension).map { size[it].toInt() }

    operator fun plus(other: FloatTensor) = initializedTensor(shape) {
        THFloatTensor_cadd(it.raw, raw, 1f, other.raw)
    }

    operator fun minus(other: FloatTensor) = initializedTensor(shape) {
        THFloatTensor_cadd(it.raw, raw, -1f, other.raw)
    }

    open operator fun times(factor: Float) = initializedTensor(shape) {
        THFloatTensor_mul(it.raw, raw, factor)
    }

    fun sum() = THFloatTensor_sumall(raw)
    fun flatten() = (0 until elements.size).map { data[it] }.toTypedArray()

    private var disposed = false
    override fun dispose() {
        if (disposed) return
        disposed = true

        THFloatTensor_free(raw)
    }

    fun asVector() = FloatVector(raw)
    fun asMatrix() = FloatMatrix(raw)
    inline fun <reified T : FloatTensor> asTensor(): T = when (T::class) {
        FloatVector::class -> asVector() as T
        FloatMatrix::class -> asMatrix() as T
        FloatTensor::class -> this as T
        else -> throw Error("Unexpected class ${T::class}")
    }

    abstract override fun toString(): String
}

class FloatVector(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    init {
        if (super.nDimension != 1)
            throw Error("A vector must have exactly 1 dimension.")
    }

    operator fun get(i: Int) = THFloatTensor_get1d(raw, i.signExtend())
    operator fun set(i: Int, value: Float) = THFloatTensor_set1d(raw, i.signExtend(), value)
    fun toArray() = (0 until shape[0]).map { i0 -> this[i0] }.toTypedArray()

    operator fun plus(other: FloatVector) = super.plus(other).asVector()
    operator fun minus(other: FloatVector) = super.minus(other).asVector()
    override operator fun times(factor: Float) = super.times(factor).asVector()
    operator fun times(other: FloatVector) = THFloatTensor_dot(raw, other.raw)

    fun abs() = kotlin.math.sqrt(this * this)

    override fun toString() = "[${toArray().joinToString { it.toString() }}]"
}

class FloatMatrix(raw: CPointer<THFloatTensor>) : FloatTensor(raw) {
    init {
        if (super.nDimension != 2)
            throw Error("A matrix must have exactly 2 dimensions.")
    }

    fun getRow(i0: Int) = (0 until shape[1]).map { i1 -> this[i0, i1] }
    operator fun get(i0: Int, i1: Int) = THFloatTensor_get2d(raw, i0.signExtend(), i1.signExtend())
    operator fun set(i0: Int, i1: Int, value: Float) = THFloatTensor_set2d(raw, i0.signExtend(), i1.signExtend(), value)
    fun toList() = (0 until shape[0]).map { getRow(it) }

    operator fun plus(other: FloatMatrix) = super.plus(other).asMatrix()
    operator fun minus(other: FloatMatrix) = super.minus(other).asMatrix()
    override operator fun times(factor: Float) = super.times(factor).asMatrix()

    operator fun times(vector: FloatVector) = initializedTensor(shape[0]) {
        THFloatTensor_addmv(it.raw, 0f, it.raw, 1f, raw, vector.raw)
    }

    operator fun times(matrix: FloatMatrix) = initializedTensor(shape[0], matrix.shape[1]) {
        THFloatTensor_addmm(it.raw, 0f, it.raw, 1f, raw, matrix.raw)
    }

    override fun toString() = "[${toList().joinToString(",\n") { "[${it.joinToString { it.toString() }}]" }}]"
}

fun uninitializedTensor(size: Int) =
        FloatVector(THFloatTensor_newWithSize1d(size.signExtend())!!)

fun uninitializedTensor(size0: Int, size1: Int) =
        FloatMatrix(THFloatTensor_newWithSize2d(size0.signExtend(), size1.signExtend())!!)

fun uninitializedTensor(shape: List<Int>) = when (shape.size) {
    1 -> uninitializedTensor(shape.single())
    2 -> uninitializedTensor(shape[0], shape[1])
    else -> throw Error("Tensors with ${shape.size} dimensions are not supported yet.")
}

fun <T> initializedTensor(size: Int, initializer: (FloatVector) -> T) =
        uninitializedTensor(size).apply { initializer(this) }

fun <T> initializedTensor(size0: Int, size1: Int, initializer: (FloatMatrix) -> T) =
        uninitializedTensor(size0, size1).apply { initializer(this) }

fun <T> initializedTensor(shape: List<Int>, initializer: (FloatTensor) -> T) =
        uninitializedTensor(shape).apply { initializer(this) }

fun tensor(size: Int, initializer: (Int) -> Float) = initializedTensor(size) {
    for (i in 0 until size) {
        it[i] = initializer(i)
    }
}

fun tensor(size0: Int, size1: Int, initializer: (Int, Int) -> Float) = initializedTensor(size0, size1) {
    for (i0 in 0 until size0) {
        for (i1 in 0 until size1) {
            it[i0, i1] = initializer(i0, i1)
        }
    }
}

fun tensor(vararg values: Float) = tensor(values.size) { values[it] }
fun tensor(vararg values: Array<Float>) = tensor(values.size, values.first().size) { i0, i1 -> values[i0][i1] }

fun full(constant: Float, size: Int) = tensor(size) { constant }
fun full(constant: Float, size0: Int, size1: Int) = tensor(size0, size1) { _, _ -> constant }
fun full(constant: Float, shape: List<Int>) = when (shape.size) {
    1 -> full(constant, shape.single())
    2 -> full(constant, shape[0], shape[1])
    else -> throw Error("Tensors with ${shape.size} dimensions are not supported yet.")
}

val randomGenerator = THGenerator_new()
fun random(min: Float, max: Float) = THRandom_uniformFloat(randomGenerator, min, max)
fun randomInt(count: Int, min: Int = 0) = random(min.toFloat(), count.toFloat()).toInt()
fun random(min: Double, max: Double, size: Int) =
        initializedTensor(size) { THFloatTensor_uniform(it.raw, randomGenerator, min, max) }

fun random(min: Double, max: Double, size0: Int, size1: Int) =
        initializedTensor(size0, size1) { THFloatTensor_uniform(it.raw, randomGenerator, min, max) }

fun zeros(size: Int) = full(0f, size)
fun zeros(size0: Int, size1: Int) = full(0f, size0, size1)
fun zeros(shape: List<Int>) = full(0f, shape)

fun ones(size: Int) = full(1f, size)
fun ones(size0: Int, size1: Int) = full(1f, size0, size1)
fun ones(shape: List<Int>) = full(1f, shape)