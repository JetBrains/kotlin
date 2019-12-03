/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

interface IMetricContainer<T> {
    fun addValue(t: T)

    fun toStringRepresentation(): String

    fun getValue(): T?

}

interface IMetricContainerFactory<T> {
    fun newMetricContainer(): IMetricContainer<T>

    //null if could not parse
    fun fromStringRepresentation(state: String): IMetricContainer<T>?
}

class OverrideMetricContainer<T>() : IMetricContainer<T> {

    private var myValue: T? = null

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

class ConcatMetricContainer() : IMetricContainer<String> {
    private val myValues = HashSet<String>()

    override fun addValue(t: String) {
        myValues.add(t)
    }

    override fun toStringRepresentation(): String {
        return myValues.joinToString(";")
    }

    override fun getValue() = toStringRepresentation()
}

