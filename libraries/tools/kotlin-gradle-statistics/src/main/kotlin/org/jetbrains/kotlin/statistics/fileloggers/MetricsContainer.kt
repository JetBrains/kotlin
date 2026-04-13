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

/**
 * A container class for managing and storing metrics of various types (numerical, boolean, and string)
 * in a structured way. This class facilitates the collection, aggregation, and processing of
 * metric data, supporting operations like reporting and reading metrics from files.
 */
class MetricsContainer(
    private val forceValuesValidation: Boolean = false,
    private val metricValuesSeparator: String = FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE,
) : StatisticsValuesConsumer {
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
        private const val METRIC_SEPARATOR = '='
        private const val PROJECT_METRIC_NAME_SEPARATOR = '.'
        private const val FUS_METRIC_SEPARATOR_FOR_KOTLIN_PROFILE_FILE = ","
        private const val FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE = ";"

        val ENCODING = Charsets.UTF_8

        private val stringMetricsMap = StringMetrics.values().associateBy(StringMetrics::name)

        private val booleanMetricsMap = BooleanMetrics.values().associateBy(BooleanMetrics::name)

        private val numericalMetricsMap = NumericalMetrics.values().associateBy(NumericalMetrics::name)

        fun createMetricsContainerForProfileFile(forceValuesValidation: Boolean = false) =
            MetricsContainer(
                forceValuesValidation = forceValuesValidation,
                metricValuesSeparator = FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE
            )

        fun createMetricsContainerForKotlinProfileFile(forceValuesValidation: Boolean = false) =
            MetricsContainer(
                forceValuesValidation = forceValuesValidation,
                metricValuesSeparator = FUS_METRIC_SEPARATOR_FOR_KOTLIN_PROFILE_FILE
            )

        private fun MetricsContainer.addMetricToContainer(
            metricDescriptor: MetricDescriptor,
            representation: String,
            separator: String = metricValuesSeparator,
        ) {
            stringMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    stringMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation, separator)
                }
            }

            booleanMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    booleanMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation, separator)
                }
            }

            numericalMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    numericalMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation, separator)
                }
            }
        }

        // for new Fus files a comma (,) separator is used, but for old FUS files a semicolon (;) is used
        private fun MetricsContainer.addMetricFromFusFile(file: File, separator: String) =
            FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.WRITE, StandardOpenOption.READ).use { channel ->
                BufferedReader(InputStreamReader(Channels.newInputStream(channel), ENCODING)).use {
                    it.forEachLine { line ->
                        if (line.contains(METRIC_SEPARATOR)) {
                            // format: metricName.hash=string representation
                            parseLine(line)?.also { (metricDescriptor, representation) ->
                                addMetricToContainer(metricDescriptor, representation, separator)
                            }
                        }
                    }
                }
            }

        fun MetricsContainer.addMetricFromFusKotlinProfileFile(file: File) =
            addMetricFromFusFile(file, separator = FUS_METRIC_SEPARATOR_FOR_KOTLIN_PROFILE_FILE)

        // This method also used IntelliJ project in KotlinGradleFUSLoggerProcessor#process.
        // It expects to read a fus file with a semicolon (;) separator for metric values.
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
                    } else if (line.contains(METRIC_SEPARATOR)) {
                        // format: metricName.hash=string representation
                        parseLine(line)?.also { (metricDescriptor, representation) ->
                            container.addMetricToContainer(metricDescriptor, representation)
                        }
                    }
                }
            } finally {
                channel.close()
            }
            return true
        }

        private fun parseLine(line: String): Pair<MetricDescriptor, String>? {
            val lineParts = line.split(METRIC_SEPARATOR)
            if (lineParts.size == 2) {
                val name = lineParts[0].split(PROJECT_METRIC_NAME_SEPARATOR)[0]
                val subProjectHash = lineParts[0].split(PROJECT_METRIC_NAME_SEPARATOR).getOrNull(1)
                val representation = lineParts[1]
                return MetricDescriptor(name, subProjectHash) to representation
            }
            return null
        }

        fun MetricsContainer.createValidateAndAnonymizeCopy(): MetricsContainer {
            val metricsContainer = MetricsContainer(forceValuesValidation, metricValuesSeparator)
            numericalMetrics.forEach { (metricDescriptor, container) ->
                val metric = numericalMetricsMap[metricDescriptor.name]
                val value = container.getValue()
                if (metric != null && value != null) {
                    metricsContainer.report(metric, value, metricDescriptor.projectHash, null)
                }
            }
            booleanMetrics.forEach { (metricDescriptor, container) ->
                val metric = booleanMetricsMap[metricDescriptor.name]
                val value = container.getValue()
                if (metric != null && value != null) {
                    metricsContainer.report(metric, value, metricDescriptor.projectHash, null)
                }
            }
            stringMetrics.forEach { (metricDescriptor, container) ->
                val metric = stringMetricsMap[metricDescriptor.name]
                val value = container.getValue()
                if (metric != null && value != null) {
                    metricsContainer.report(metric, value, metricDescriptor.projectHash, null)
                }
            }
            return metricsContainer
        }
    }


    private fun processProjectName(subprojectName: String?, perProject: Boolean) =
        if (perProject && subprojectName != null) sha256(subprojectName) else null

    private fun getProjectHash(perProject: Boolean, subprojectName: String?) =
        if (subprojectName == null) null else processProjectName(subprojectName, perProject)

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = booleanMetrics.getOrPut(MetricDescriptor(metric.name, projectHash)) { metric.type.newMetricContainer() }
            metricContainer.addValue(metric.anonymization.anonymize(value, metricValuesSeparator), weight)
        }
        return true
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = numericalMetrics.getOrPut(MetricDescriptor(metric.name, projectHash)) { metric.type.newMetricContainer() }
            metricContainer.addValue(metric.anonymization.anonymize(value, metricValuesSeparator), weight)
        }
        return true
    }

    internal fun validateMetric(metric: StringMetrics, value: String) {
        if (value.contains(UNEXPECTED_VALUE) || !value.matches(Regex(metric.anonymization.validationRegexp(metricValuesSeparator)))) {
            throw MetricValueValidationFailed("Metric ${metric.name} has value [${value}]. Validation regex: ${metric.anonymization.validationRegexp()}.")
        }
    }

    internal fun anonymizeMetric(metric: StringMetrics, value: String) = metric.anonymization.anonymize(value, metricValuesSeparator)

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = stringMetrics.getOrPut(MetricDescriptor(metric.name, projectHash)) { metric.type.newMetricContainer() }

            val anonymizedValue = anonymizeMetric(metric, value)
            if (forceValuesValidation && !metric.anonymization.anonymizeOnIdeSize()) {
                validateMetric(metric, anonymizedValue)
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
            writer.appendLine("${entry.key.name}$suffix$METRIC_SEPARATOR${entry.value.toStringRepresentation(metricValuesSeparator)}")
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

    fun getStringMetricPresentation(metric: StringMetrics): String? = getMetric(metric)?.toStringRepresentation(metricValuesSeparator)

    fun getMetric(metric: BooleanMetrics): IMetricContainer<Boolean>? = synchronized(metricsLock) {
        booleanMetrics[MetricDescriptor(metric.name, null)]
    }

    fun isEmpty(): Boolean = synchronized(metricsLock) {
        numericalMetrics.isEmpty() && booleanMetrics.isEmpty() && stringMetrics.isEmpty()
    }
}
