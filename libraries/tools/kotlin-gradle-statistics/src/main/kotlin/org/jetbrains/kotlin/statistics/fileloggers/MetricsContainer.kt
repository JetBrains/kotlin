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
    private val metricConcatContainerValuesSeparator: String = FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE,
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

    private val stringListMetrics = TreeMap<MetricDescriptor, IMetricContainer<List<String>>>()

    companion object {
        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"
        private const val METRIC_SEPARATOR = '='
        private const val PROJECT_METRIC_NAME_SEPARATOR = '.'
        private const val FUS_METRIC_SEPARATOR_FOR_KOTLIN_PROFILE_FILE = ","
        private const val FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE = ";"

        val ENCODING = Charsets.UTF_8

        private val stringMetricsMap = StringMetrics.values().associateBy(StringMetrics::name)

        private val stringListMetricsMap = StringListMetrics.values().associateBy(StringListMetrics::name)

        private val booleanMetricsMap = BooleanMetrics.values().associateBy(BooleanMetrics::name)

        private val numericalMetricsMap = NumericalMetrics.values().associateBy(NumericalMetrics::name)

        fun createMetricsContainerForProfileFile(forceValuesValidation: Boolean = false) =
            MetricsContainer(
                forceValuesValidation = forceValuesValidation,
                metricConcatContainerValuesSeparator = FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE
            )

        fun createMetricsContainerForKotlinProfileFile(forceValuesValidation: Boolean = false) =
            MetricsContainer(
                forceValuesValidation = forceValuesValidation,
                metricConcatContainerValuesSeparator = FUS_METRIC_SEPARATOR_FOR_KOTLIN_PROFILE_FILE
            )

        private fun MetricsContainer.addMetricToContainer(
            metricDescriptor: MetricDescriptor,
            representation: String,
        ) {
            stringMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    stringMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation)
                }
            }

            stringListMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    stringListMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation)
                }
            }

            booleanMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    booleanMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation)
                }
            }

            numericalMetricsMap[metricDescriptor.name]?.also { metricType ->
                synchronized(metricsLock) {
                    numericalMetrics.getOrPut(metricDescriptor) {
                        metricType.type.newMetricContainer()
                    }.addValueFromStringPresentation(representation)
                }
            }
        }

        // for new Fus files a comma (,) separator is used, but for old FUS files a semicolon (;) is used
        fun MetricsContainer.addMetricFromFusKotlinProfileFile(file: File) {
            copy(fromContainer = deserializeKotlinProfileMetricsContainer(file))
        }

        // Old format: fun deserializeProfileFileContainersStream(file: File): Stream<MetricsContainer>
        // New format: fun deserializeKotlinProfileMetricsContainer(file: File): MetricsContainer
        // fun relay(fromContainer: MetricsContainer, toContainer: MetricsContainer)

        fun deserializeKotlinProfileMetricsContainer(file: File): MetricsContainer {
            val container = MetricsContainer(metricConcatContainerValuesSeparator = FUS_METRIC_SEPARATOR_FOR_KOTLIN_PROFILE_FILE)
            FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.WRITE, StandardOpenOption.READ).use { channel ->
                BufferedReader(InputStreamReader(Channels.newInputStream(channel), ENCODING)).use {
                    it.forEachLine { line ->
                        if (line.contains(METRIC_SEPARATOR)) {
                            // format: metricName.hash=string representation
                            parseLine(line)?.also { (metricDescriptor, representation) ->
                                container.addMetricToContainer(metricDescriptor, representation)
                            }
                        }
                    }
                }
            }
            return container
        }

        fun deserializeProfileFileContainersStream(channel: FileChannel): List<MetricsContainer> {
            val containers = mutableListOf<MetricsContainer>()
            val inputStream = Channels.newInputStream(channel)
            var container =
                MetricsContainer(metricConcatContainerValuesSeparator = FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE)
            // Note: close is called at forEachLine
            BufferedReader(InputStreamReader(inputStream, ENCODING)).forEachLine { line ->
                if (BUILD_SESSION_SEPARATOR == line) {
                    containers.add(container)
                    container =
                        MetricsContainer(metricConcatContainerValuesSeparator = FUS_METRIC_SEPARATOR_FOR_BACKWARD_COMPATIBILITY_PROFILE_FILE)
                } else if (line.contains(METRIC_SEPARATOR)) {
                    // format: metricName.hash=string representation
                    parseLine(line)?.also { (metricDescriptor, representation) ->
                        container.addMetricToContainer(
                            metricDescriptor,
                            representation,
                        )
                    }
                }
            }
            return containers
        }

        fun MetricsContainer.copy(fromContainer: MetricsContainer) {
            val toContainer = this
            fromContainer.stringMetrics.forEach { (key, metricContainer) ->
                metricContainer.getValue()?.let { value ->
                    toContainer.stringMetrics.getOrPut(key, { stringMetricsMap[key.name]?.type!!.newMetricContainer() })
                        .addValue(value, null)
                }
            }
            fromContainer.stringListMetrics.forEach { (key, metricContainer) ->
                metricContainer.getValue()?.let { value ->
                    toContainer.stringListMetrics.getOrPut(key, { stringListMetricsMap[key.name]?.type!!.newMetricContainer() })
                        .addValue(value, null)
                }
            }
            fromContainer.numericalMetrics.forEach { (key, metricContainer) ->
                metricContainer.getValue()?.let { value ->
                    toContainer.numericalMetrics.getOrPut(key, { numericalMetricsMap[key.name]?.type!!.newMetricContainer() })
                        .addValue(value, null)
                }
            }
            fromContainer.booleanMetrics.forEach { (key, metricContainer) ->
                metricContainer.getValue()?.let { value ->
                    toContainer.booleanMetrics.getOrPut(key, { booleanMetricsMap[key.name]?.type!!.newMetricContainer() })
                        .addValue(value, null)
                }
            }
        }

        // This method also used IntelliJ project in KotlinGradleFUSLoggerProcessor#process.
        // It expects to read a fus file with a semicolon (;) separator for metric values.
        fun readFromFile(file: File, consumer: (MetricsContainer) -> Unit): Boolean {
            val channel = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.WRITE, StandardOpenOption.READ)
            channel.tryLock() ?: return false
            try {
                deserializeProfileFileContainersStream(channel).forEach {
                    consumer(it)
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
            val metricsContainer = MetricsContainer(forceValuesValidation, metricConcatContainerValuesSeparator)
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
            stringListMetrics.forEach { (metricDescriptor, container) ->
                val metric = stringListMetricsMap[metricDescriptor.name]
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
            metricContainer.addValue(metric.anonymization.anonymize(value, metricConcatContainerValuesSeparator), weight)
        }
        return true
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer = numericalMetrics.getOrPut(MetricDescriptor(metric.name, projectHash)) { metric.type.newMetricContainer() }
            metricContainer.addValue(metric.anonymization.anonymize(value, metricConcatContainerValuesSeparator), weight)
        }
        return true
    }

    override fun report(metric: StringListMetrics, value: List<String>, subprojectName: String?, weight: Long?): Boolean {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        synchronized(metricsLock) {
            val metricContainer =
                stringListMetrics.getOrPut(MetricDescriptor(metric.name, projectHash)) { metric.type.newMetricContainer() }
            metricContainer.addValue(value.map { metric.anonymization.anonymize(it, metricConcatContainerValuesSeparator) }, weight)
        }
        return true
    }

    internal fun validateMetric(metric: StringMetrics, value: String) {
        if (value.contains(UNEXPECTED_VALUE) || !value.matches(
                Regex(
                    metric.anonymization.validationRegexp(
                        metricConcatContainerValuesSeparator
                    )
                )
            )
        ) {
            throw MetricValueValidationFailed("Metric ${metric.name} has value [${value}]. Validation regex: ${metric.anonymization.validationRegexp()}.")
        }
    }

    internal fun anonymizeMetric(metric: StringMetrics, value: String) =
        metric.anonymization.anonymize(value, metricConcatContainerValuesSeparator)

    fun StringListOverridePolicy.newMetricContainer(): IMetricContainer<List<String>> = when (this) {
        StringListOverridePolicy.CONCAT -> ConcatMetricContainer(metricConcatContainerValuesSeparator)
    }

    fun StringOverridePolicy.newMetricContainer() = when (this) {
        StringOverridePolicy.OVERRIDE -> OverrideStringMetricContainer()
        StringOverridePolicy.OVERRIDE_VERSION_IF_NOT_SET -> OverrideVersionMetricContainer()
    }

    fun BooleanOverridePolicy.newMetricContainer() = when (this) {
        BooleanOverridePolicy.OVERRIDE -> OverrideBooleanMetricContainer()
        BooleanOverridePolicy.OR -> OrMetricContainer()
    }

    fun NumberOverridePolicy.newMetricContainer() = when (this) {
        NumberOverridePolicy.OVERRIDE -> OverrideLongMetricContainer()
        NumberOverridePolicy.SUM -> SumMetricContainer()
        NumberOverridePolicy.AVERAGE -> AverageMetricContainer()
    }

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
            allMetrics.putAll(stringListMetrics)
        }
        writer.appendLine()
        for (entry in allMetrics.entries) {
            val suffix = if (entry.key.projectHash == null) "" else ".${entry.key.projectHash}"
            writer.appendLine("${entry.key.name}$suffix$METRIC_SEPARATOR${entry.value.toStringRepresentation()}")
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

    fun getStringMetricPresentation(metric: StringMetrics): String? = getMetric(metric)?.toStringRepresentation()

    fun getMetric(metric: BooleanMetrics): IMetricContainer<Boolean>? = synchronized(metricsLock) {
        booleanMetrics[MetricDescriptor(metric.name, null)]
    }

    fun getMetric(metric: StringListMetrics): IMetricContainer<List<String>>? = synchronized(metricsLock) {
        stringListMetrics[MetricDescriptor(metric.name, null)]
    }

    fun isEmpty(): Boolean = synchronized(metricsLock) {
        numericalMetrics.isEmpty() && booleanMetrics.isEmpty() && stringMetrics.isEmpty()
    }
}
