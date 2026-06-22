package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
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

    private val numericalMetrics = HashMap<MetricDescriptor<NumericalMetrics>, MutableList<Pair<Long, Long?>>>()

    private val booleanMetrics = HashMap<MetricDescriptor<BooleanMetrics>, MutableList<Pair<Boolean, Long?>>>()

    private val stringMetrics = HashMap<MetricDescriptor<StringMetrics>, MutableList<Pair<String, Long?>>>()

    private fun <V, K : MetricDescriptor<*>> putToCollection(
        collection: MutableMap<K, MutableList<Pair<V, Long?>>>,
        key: K,
        value: V,
        weight: Long? = null,
    ) {
        collection.getOrPut(key) { ArrayList<Pair<V, Long?>>() }.add(value to weight)
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        putToCollection(booleanMetrics, MetricDescriptor(metric, subprojectName), value, weight)
        return true
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        putToCollection(numericalMetrics, MetricDescriptor(metric, subprojectName), value, weight)
        return true
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean {
        putToCollection(stringMetrics, MetricDescriptor(metric, subprojectName), value, weight)
        return true
    }

    open fun readFromMetricConsumer(metricConsumer: NonSynchronizedMetricsContainer) {
        metricConsumer.booleanMetrics.forEach {
            it.value.forEach { (value, weight) -> putToCollection(booleanMetrics, MetricDescriptor(it.key.name, it.key.subprojectName), value, weight) }
        }
        metricConsumer.stringMetrics.forEach {
            it.value.forEach { (value, weight) -> putToCollection(stringMetrics, MetricDescriptor(it.key.name, it.key.subprojectName), value, weight) }
        }
        metricConsumer.numericalMetrics.forEach {
            it.value.forEach { (value, weight) -> putToCollection(numericalMetrics, MetricDescriptor(it.key.name, it.key.subprojectName), value, weight) }
        }
    }

    open fun sendToConsumer(metricConsumer: StatisticsValuesConsumer) {
        booleanMetrics.forEach {
            it.value.forEach { (value, weight) -> metricConsumer.report(it.key.name, value, it.key.subprojectName, weight) }
        }
        numericalMetrics.forEach {
            it.value.forEach { (value, weight) -> metricConsumer.report(it.key.name, value, it.key.subprojectName, weight) }
        }
        stringMetrics.forEach {
            it.value.forEach { (value, weight) -> metricConsumer.report(it.key.name, value, it.key.subprojectName, weight) }
        }
    }
}
