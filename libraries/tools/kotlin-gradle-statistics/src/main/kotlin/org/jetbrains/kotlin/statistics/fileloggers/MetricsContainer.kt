/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.fileloggers

import org.jetbrains.kotlin.statistics.MetricValueValidationFailed
import org.jetbrains.kotlin.statistics.metrics.*
import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy.AllowedListAnonymizer.Companion.UNEXPECTED_VALUE
import org.jetbrains.kotlin.statistics.sha256
import java.io.*
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

class MetricsContainer(private val forceValuesValidation: Boolean = false) : StatisticsValuesConsumer {
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

    private val metricsLock = Object()

    private val numericalMetrics = TreeMap<MetricDescriptor, IMetricContainer<Long>>()

    private val booleanMetrics = TreeMap<MetricDescriptor, IMetricContainer<Boolean>>()

    private val stringMetrics = TreeMap<MetricDescriptor, IMetricContainer<String>>()

    companion object {
        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"

        val ENCODING = Charsets.UTF_8

        private val stringMetricsMap = StringMetrics.values().associateBy(StringMetrics::name)

        private val booleanMetricsMap = BooleanMetrics.values().associateBy(BooleanMetrics::name)

        private val numericalMetricsMap = NumericalMetrics.values().associateBy(NumericalMetrics::name)

        fun readFromFile(file: File, consumer: (MetricsContainer) -> Unit): Boolean {
            val channel = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.WRITE, StandardOpenOption.READ)
            channel.tryLock() ?: return false

            val inputStream = Channels.newInputStream(channel)
            try {
                var container = MetricsContainer()
                // Note: close is called at forEachLine
                BufferedReader(InputStreamReader(inputStream, ENCODING)).forEachLine { line ->
                    if (BUILD_SESSION_SEPARATOR == line) {
                        consumer.invoke(container)
                        container = MetricsContainer()
                    } else {
                        // format: metricName.hash=string representation
                        val lineParts = line.split('=')
                        if (lineParts.size == 2) {
                            val name = lineParts[0].split('.')[0]
                            val subProjectHash = lineParts[0].split('.').getOrNull(1)
                            val representation = lineParts[1]

                            stringMetricsMap[name]?.also { metricType ->
                                metricType.type.fromStringRepresentation(representation)?.also {
                                    synchronized(container.metricsLock) {
                                        container.stringMetrics[MetricDescriptor(name, subProjectHash)] = it
                                    }
                                }
                            }

                            booleanMetricsMap[name]?.also { metricType ->
                                metricType.type.fromStringRepresentation(representation)?.also {
                                    synchronized(container.metricsLock) {
                                        container.booleanMetrics[MetricDescriptor(name, subProjectHash)] = it
                                    }
                                }
                            }

                            numericalMetricsMap[name]?.also { metricType ->
                                metricType.type.fromStringRepresentation(representation)?.also {
                                    synchronized(container.metricsLock) {
                                        container.numericalMetrics[MetricDescriptor(name, subProjectHash)] = it
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                channel.close()
            }
            return true
        }
    }

    private fun processProjectName(subprojectName: String?, perProject: Boolean) =
        if (perProject && subprojectName != null) sha256(subprojectName) else null

    private fun getProjectHash(perProject: Boolean, subprojectName: String?) =
        if (subprojectName == null) null else processProjectName(subprojectName, perProject)

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = booleanMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
                .also { booleanMetrics[MetricDescriptor(metric.name, projectHash)] = it }
            metricContainer.addValue(metric.anonymization.anonymize(value), weight)
        }
        return true
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = numericalMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
                .also { numericalMetrics[MetricDescriptor(metric.name, projectHash)] = it }
            metricContainer.addValue(metric.anonymization.anonymize(value), weight)
        }
        return true
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = stringMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
                .also { stringMetrics[MetricDescriptor(metric.name, projectHash)] = it }

            val anonymizedValue = metric.anonymization.anonymize(value)
            if (forceValuesValidation && !metric.anonymization.anonymizeOnIdeSize()) {
                if (anonymizedValue.contains(UNEXPECTED_VALUE) || !anonymizedValue.matches(Regex(metric.anonymization.validationRegexp()))) {
                    throw MetricValueValidationFailed("Metric ${metric.name} has value [${value}], after anonymization [${anonymizedValue}]. Validation regex: ${metric.anonymization.validationRegexp()}.")
                }
            }
            metricContainer.addValue(anonymizedValue, weight)
        }
        return true
    }

    fun flush(writer: BufferedWriter) {
        val allMetrics = TreeMap<MetricDescriptor, IMetricContainer<out Any>>()
        synchronized(metricsLock) {
            allMetrics.putAll(numericalMetrics)
            allMetrics.putAll(booleanMetrics)
            allMetrics.putAll(stringMetrics)
        }
        writer.appendLine()
        for (entry in allMetrics.entries) {
            val suffix = if (entry.key.projectHash == null) "" else ".${entry.key.projectHash}"
            writer.appendLine("${entry.key.name}$suffix=${entry.value.toStringRepresentation()}")
        }

        writer.appendLine(BUILD_SESSION_SEPARATOR)

        synchronized(metricsLock) {
            stringMetrics.clear()
            booleanMetrics.clear()
            numericalMetrics.clear()
        }
    }

    fun getMetric(metric: NumericalMetrics): IMetricContainer<Long>? = synchronized(metricsLock) {
        numericalMetrics[MetricDescriptor(metric.name, null)]
    }

    fun getMetric(metric: StringMetrics): IMetricContainer<String>? = synchronized(metricsLock) {
        stringMetrics[MetricDescriptor(metric.name, null)]
    }

    fun getMetric(metric: BooleanMetrics): IMetricContainer<Boolean>? = synchronized(metricsLock) {
        booleanMetrics[MetricDescriptor(metric.name, null)]
    }
}
