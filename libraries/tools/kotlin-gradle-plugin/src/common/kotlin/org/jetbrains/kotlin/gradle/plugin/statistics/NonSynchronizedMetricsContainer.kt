package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.statistics.metrics.*
import java.io.Serializable

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal open class NonSynchronizedMetricsContainer : StatisticsValuesConsumer, Serializable {
    data class MetricDescriptor<T : Comparable<T>>(val name: T, val subprojectName: String?) : Comparable<MetricDescriptor<T>> {
        override fun compareTo(other: MetricDescriptor<T>): Int {
            val compareNames = name.compareTo(other.name)
            return when {
                compareNames != 0 -> compareNames
                subprojectName == other.subprojectName -> 0
                else -> (subprojectName ?: "").compareTo(other.subprojectName ?: "")
            }
        }
    }

    private val numericalMetrics = HashMap<MetricDescriptor<NumericalMetrics>, IMetricContainer<Long>>()

    private val booleanMetrics = HashMap<MetricDescriptor<BooleanMetrics>, IMetricContainer<Boolean>>()

    private val stringMetrics = HashMap<MetricDescriptor<StringMetrics>, IMetricContainer<String>>()

    private fun <V, T : IMetricContainerFactory<V>, K : MetricDescriptor<*>> putToCollection(
        collection: MutableMap<K, IMetricContainer<V>>,
        key: K,
        type: T,
        value: V,
        weight: Long? = null,
    ) {
        collection.getOrPut(key) { type.newMetricContainer() }.addValue(value, weight)
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        putToCollection(booleanMetrics, MetricDescriptor(metric, subprojectName), metric.type, value, weight)
        return true
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        putToCollection(numericalMetrics, MetricDescriptor(metric, subprojectName), metric.type, value, weight)
        return true
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean {
        putToCollection(stringMetrics, MetricDescriptor(metric, subprojectName), metric.type, value, weight)
        return true
    }

    open fun readFromMetricConsumer(metricConsumer: NonSynchronizedMetricsContainer) {
        metricConsumer.booleanMetrics.forEach {
            putToCollection(booleanMetrics, MetricDescriptor(it.key.name, it.key.subprojectName), it.key.name.type, it.value.getValue()!!)
        }
        metricConsumer.stringMetrics.forEach {
            putToCollection(stringMetrics, MetricDescriptor(it.key.name, it.key.subprojectName), it.key.name.type, it.value.getValue()!!)
        }
        metricConsumer.numericalMetrics.forEach {
            putToCollection(numericalMetrics, MetricDescriptor(it.key.name, it.key.subprojectName), it.key.name.type, it.value.getValue()!!)
        }
    }

    open fun sendToConsumer(metricConsumer: StatisticsValuesConsumer) {
            booleanMetrics.forEach { metricConsumer.report(it.key.name, it.value.getValue()!!, it.key.subprojectName) }
            numericalMetrics.forEach { metricConsumer.report(it.key.name, it.value.getValue()!!, it.key.subprojectName) }
            stringMetrics.forEach { metricConsumer.report(it.key.name, it.value.getValue()!!, it.key.subprojectName) }
    }
}