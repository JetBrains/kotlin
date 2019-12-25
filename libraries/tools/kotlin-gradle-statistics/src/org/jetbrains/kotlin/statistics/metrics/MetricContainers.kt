/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import java.util.*

interface IMetricContainer<T> {
    fun addValue(t: T)

    fun toStringRepresentation(): String

    fun getValue(): T?

}

interface IMetricContainerFactory<T> {
    fun newMetricContainer(): IMetricContainer<T>

    fun fromStringRepresentation(state: String): IMetricContainer<T>?
}

open class OverrideMetricContainer<T>() : IMetricContainer<T> {

    internal var myValue: T? = null

    override fun addValue(t: T) {
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

class SumMetricContainer() : OverrideMetricContainer<Long>() {

    constructor(v: Long) : this() {
        myValue = v
    }

    override fun addValue(t: Long) {
        myValue = (myValue ?: 0) + t
    }
}

class AverageMetricContainer() : IMetricContainer<Long> {
    private var count = 0
    private var myValue: Long? = null

    constructor(v: Long) : this() {
        myValue = v
    }

    override fun addValue(t: Long) {
        myValue = (myValue ?: 0) + t
        count++
    }

    override fun toStringRepresentation(): String {
        return getValue()?.toString() ?: "null"
    }

    override fun getValue(): Long? {
        return myValue?.div(count)
    }
}

class OrMetricContainer() : OverrideMetricContainer<Boolean>() {
    constructor(v: Boolean) : this() {
        myValue = v
    }

    override fun addValue(t: Boolean) {
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

    override fun addValue(t: String) {
        myValues.add(t.replace(SEPARATOR, ","))
    }

    override fun toStringRepresentation(): String {
        return myValues.joinToString(SEPARATOR)
    }

    override fun getValue() = toStringRepresentation()
}

