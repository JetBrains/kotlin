/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.fileloggers

import org.jetbrains.kotlin.statistics.metrics.*
import org.jetbrains.kotlin.statistics.sha256
import java.io.*
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

class MetricsContainer : IStatisticsValuesConsumer {
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
                                    container.stringMetrics[MetricDescriptor(name, subProjectHash)] = it
                                }
                            }

                            booleanMetricsMap[name]?.also { metricType ->
                                metricType.type.fromStringRepresentation(representation)?.also {
                                    container.booleanMetrics[MetricDescriptor(name, subProjectHash)] = it
                                }
                            }

                            numericalMetricsMap[name]?.also { metricType ->
                                metricType.type.fromStringRepresentation(representation)?.also {
                                    container.numericalMetrics[MetricDescriptor(name, subProjectHash)] = it
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

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?) {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        val metricContainer = booleanMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
            .also { booleanMetrics[MetricDescriptor(metric.name, projectHash)] = it }
        metricContainer.addValue(metric.anonymization.anonymize(value))
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?) {
        val projectHash = getProjectHash(metric.perProject, subprojectName)
        val metricContainer = numericalMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
            .also { numericalMetrics[MetricDescriptor(metric.name, projectHash)] = it }
        metricContainer.addValue(metric.anonymization.anonymize(value))
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?) {
        val projectHash = if (subprojectName == null) null else processProjectName(subprojectName, metric.perProject)
        val metricContainer = stringMetrics[MetricDescriptor(metric.name, projectHash)] ?: metric.type.newMetricContainer()
            .also { stringMetrics[MetricDescriptor(metric.name, projectHash)] = it }
        metricContainer.addValue(metric.anonymization.anonymize(value))
    }

    fun flush(trackingFile: IRecordLogger?) {
        if (trackingFile == null) return
        for (entry in numericalMetrics.entries.union(booleanMetrics.entries).union(stringMetrics.entries)) {
            val suffix = if (entry.key.projectHash == null) "" else ".${entry.key.projectHash}"
            trackingFile.append("${entry.key.name}$suffix=${entry.value.toStringRepresentation()}")
        }

        trackingFile.append(BUILD_SESSION_SEPARATOR)

        stringMetrics.clear()
        booleanMetrics.clear()
        numericalMetrics.clear()
    }

    fun getMetric(metric: NumericalMetrics): IMetricContainer<Long>? = numericalMetrics[MetricDescriptor(metric.name, null)]

    fun getMetric(metric: StringMetrics): IMetricContainer<String>? = stringMetrics[MetricDescriptor(metric.name, null)]

    fun getMetric(metric: BooleanMetrics): IMetricContainer<Boolean>? = booleanMetrics[MetricDescriptor(metric.name, null)]
}
