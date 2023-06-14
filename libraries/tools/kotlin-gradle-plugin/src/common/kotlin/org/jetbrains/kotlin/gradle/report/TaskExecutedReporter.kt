/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics

class TaskExecutedReporter : BuildReportService {
    override fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters
    ) {
        //Do nothing
    }

    override fun onFinish(
        event: TaskFinishEvent,
        buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?
    ) {
        KotlinBuildStatsService.applyIfInitialised { reportTaskIfNeed(event.descriptor.name) }
    }

    private fun reportTaskIfNeed(task: String) {
        val metric = when (task.substringAfterLast(":")) {
            "dokkaHtml" -> BooleanMetrics.ENABLED_DOKKA_HTML
            "dokkaGfm" -> BooleanMetrics.ENABLED_DOKKA_GFM
            "dokkaJavadoc" -> BooleanMetrics.ENABLED_DOKKA_JAVADOC
            "dokkaJekyll" -> BooleanMetrics.ENABLED_DOKKA_JEKYLL
            "dokkaHtmlMultiModule" -> BooleanMetrics.ENABLED_DOKKA_HTML_MULTI_MODULE
            "dokkaGfmMultiModule" -> BooleanMetrics.ENABLED_DOKKA_GFM_MULTI_MODULE
            "dokkaJekyllMultiModule" -> BooleanMetrics.ENABLED_DOKKA_JEKYLL_MULTI_MODULE
            "dokkaHtmlCollector" -> BooleanMetrics.ENABLED_DOKKA_HTML_COLLECTOR
            "dokkaGfmCollector" -> BooleanMetrics.ENABLED_DOKKA_GFM_COLLECTOR
            "dokkaJavadocCollector" -> BooleanMetrics.ENABLED_DOKKA_JAVADOC_COLLECTOR
            "dokkaJekyllCollector" -> BooleanMetrics.ENABLED_DOKKA_JEKYLL_COLLECTOR
            else -> null
        }
        metric?.also { KotlinBuildStatsService.getInstance()?.report(it, true) }
    }
}