/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import java.io.Serializable
import java.util.*

// This container is a value in a key/value pair such as MPP_PLATFORMS=...
interface IMetricContainer<T> : Serializable {
    fun addValue(t: T, weight: Long? = null)

    fun addValueFromStringPresentation(str: String)

    fun toStringRepresentation(): String

    fun getValue(): T?
}

abstract class OverrideMetricContainer<T>() : IMetricContainer<T> {
    internal var myValue: T? = null

    override fun addValue(t: T, weight: Long?) {
        myValue = t
    }

    override fun toStringRepresentation(): String {
        return myValue?.toString() ?: "null"
    }

    override fun getValue() = myValue
}

open class OverrideStringMetricContainer() : OverrideMetricContainer<String>() {
    override fun addValueFromStringPresentation(str: String) {
        addValue(str, null)
    }
}

open class OverrideLongMetricContainer() : OverrideMetricContainer<Long>() {
    override fun addValueFromStringPresentation(str: String) {
        str.toLongOrNull()?.also { addValue(it, null) }
    }
}

open class OverrideBooleanMetricContainer(): OverrideMetricContainer<Boolean>() {
    override fun addValueFromStringPresentation(str: String) {
        str.toBooleanStrictOrNull()?.also { addValue(it, null) }
    }
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

    override fun addValueFromStringPresentation(str: String) {
        addValue(str, null)
    }
}

class SumMetricContainer() : OverrideMetricContainer<Long>() {
    constructor(v: Long) : this() {
        myValue = v
    }

    override fun addValue(t: Long, weight: Long?) {
        myValue = (myValue ?: 0) + t
    }

    override fun addValueFromStringPresentation(str: String) {
        str.toLongOrNull()?.also { addValue(it, null) }
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

    override fun addValueFromStringPresentation(str: String) {
        str.toLongOrNull()?.also { addValue(it, null) }
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

    override fun addValueFromStringPresentation(str: String) {
        str.toBooleanStrictOrNull()?.also { addValue(it, null) }
    }
}

class ConcatMetricContainer(val separator: String) : IMetricContainer<List<String>> {
    private val myValues = TreeSet<String>()

    override fun addValue(t: List<String>, weight: Long?) {
        myValues.addAll(t)
    }

    override fun addValueFromStringPresentation(str: String) {
        addValue(str.split(separator), null)
    }

    override fun toStringRepresentation(): String {
        return myValues.sorted().joinToString(separator)
    }

    override fun getValue() = myValues.toList()
}
