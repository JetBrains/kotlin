/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.fileloggers

import org.jetbrains.kotlin.statistics.metrics.*
import org.jetbrains.kotlin.statistics.sha256
import java.io.Serializable
import java.util.*

open class NonSynchronizedMetricsContainer : IStatisticsValuesConsumer, Serializable {
    data class MetricDescriptor(val name: String, val projectHash: String?) : Comparable<MetricDescriptor> {
        override fun compareTo(other: MetricDescriptor): Int {
            val compareNames = name.compareTo(other.name)
            return when {
                compareNames != 0 -> compareNames
                projectHash == other.projectHash -> 0
                else -> (projectHash ?: "").compareTo(other.projectHash ?: "")
            }
        }
    }

    protected val numericalMetrics = TreeMap<MetricDescriptor, IMetricContainer<Long>>()

    protected val booleanMetrics = TreeMap<MetricDescriptor, IMetricContainer<Boolean>>()

    protected val stringMetrics = TreeMap<MetricDescriptor, IMetricContainer<String>>()

    private fun processProjectName(subprojectName: String?, perProject: Boolean) =
        if (perProject && subprojectName != null) sha256(subprojectName) else null

    private fun getProjectHash(perProject: Boolean, subprojectName: String?) =
        if (subprojectName == null) null else processProjectName(subprojectName, perProject)

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        val metricContainer = booleanMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
            .also { booleanMetrics[MetricDescriptor(metric.name, projectHash)] = it }
        metricContainer.addValue(metric.anonymization.anonymize(value), weight)
        return true
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        val metricContainer = numericalMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
            .also { numericalMetrics[MetricDescriptor(metric.name, projectHash)] = it }
        metricContainer.addValue(metric.anonymization.anonymize(value), weight)
        return true
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        val metricContainer = stringMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
            .also { stringMetrics[MetricDescriptor(metric.name, projectHash)] = it }

        val anonymizedValue = metric.anonymization.anonymize(value)
        metricContainer.addValue(anonymizedValue, weight)
        return true
    }
}