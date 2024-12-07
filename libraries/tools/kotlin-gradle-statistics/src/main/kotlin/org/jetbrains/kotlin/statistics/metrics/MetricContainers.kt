/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import java.io.Serializable
import java.util.*

interface IMetricContainer<T> : Serializable {
    fun addValue(t: T, weight: Long? = null)

    fun toStringRepresentation(): String

    fun getValue(): T?
}

interface IMetricContainerFactory<T> {
    fun newMetricContainer(): IMetricContainer<T>

    fun fromStringRepresentation(state: String): IMetricContainer<T>?
}

open class OverrideMetricContainer<T>() : IMetricContainer<T> {
    internal var myValue: T? = null

    override fun addValue(t: T, weight: Long?) {
        myValue = t
    }

    internal constructor(v: T?) : this() {
        myValue = v
    }

    override fun toStringRepresentation(): String {
        return myValue?.toString() ?: "null"
    }

    override fun getValue() = myValue
}

class OverrideVersionMetricContainer() : OverrideMetricContainer<String>() {
    constructor(v: String) : this() {
        myValue = v
    }

    override fun addValue(t: String, weight: Long?) {
        if (myValue == null || myValue == "0.0.0") {
            myValue = t
        }
    }
}

class SumMetricContainer() : OverrideMetricContainer<Long>() {
    constructor(v: Long) : this() {
        myValue = v
    }

    override fun addValue(t: Long, weight: Long?) {
        myValue = (myValue ?: 0) + t
    }
}

class AverageMetricContainer() : IMetricContainer<Long> {
    private var totalWeight = 0L
    private var totalSum: Long? = null

    constructor(v: Long) : this() {
        totalSum = v
        totalWeight = 1
    }

    override fun addValue(t: Long, weight: Long?) {
        val w = weight ?: 1
        totalSum = (totalSum ?: 0) + t * w
        totalWeight += w
    }

    override fun toStringRepresentation(): String {
        return getValue()?.toString() ?: "null"
    }

    override fun getValue(): Long? {
        return totalSum?.div(if (totalWeight > 0) totalWeight else 1)
    }
}

class OrMetricContainer() : OverrideMetricContainer<Boolean>() {
    constructor(v: Boolean) : this() {
        myValue = v
    }

    override fun addValue(t: Boolean, weight: Long?) {
        myValue = (myValue ?: false) || t
    }
}

class ConcatMetricContainer() : IMetricContainer<String> {
    private val myValues = TreeSet<String>()

    companion object {
        const val SEPARATOR = ";"
    }

    constructor(values: Collection<String>) : this() {
        myValues.addAll(values)
    }

    override fun addValue(t: String, weight: Long?) {
        myValues.add(t.replace(SEPARATOR, ","))
    }

    override fun toStringRepresentation(): String {
        return myValues.sorted().joinToString(SEPARATOR)
    }

    override fun getValue() = toStringRepresentation()
}
