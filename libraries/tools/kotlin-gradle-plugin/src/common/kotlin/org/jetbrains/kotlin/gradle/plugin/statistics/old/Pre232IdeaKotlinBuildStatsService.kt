/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.old

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.statistics.AbstractKotlinBuildStatsService
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatHandler
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import javax.management.ObjectName

internal class Pre232IdeaKotlinBuildStatsService internal constructor(
    gradle: Gradle,
    beanName: ObjectName
) : AbstractKotlinBuildStatsService(gradle, beanName), Pre232IdeaKotlinBuildStatsMXBean {

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean =
        KotlinBuildStatHandler().report(sessionLogger, metric, value, subprojectName, weight)

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean =
        KotlinBuildStatHandler().report(sessionLogger, metric, value, subprojectName, weight)

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean =
        KotlinBuildStatHandler().report(sessionLogger, metric, value, subprojectName, weight)

    override fun reportBoolean(name: String, value: Boolean, subprojectName: String?, weight: Long?): Boolean =
        report(BooleanMetrics.valueOf(name), value, subprojectName, weight)

    override fun reportNumber(name: String, value: Long, subprojectName: String?, weight: Long?): Boolean =
        report(NumericalMetrics.valueOf(name), value, subprojectName, weight)

    override fun reportString(name: String, value: String, subprojectName: String?, weight: Long?): Boolean =
        report(StringMetrics.valueOf(name), value, subprojectName, weight)

    override fun reportBoolean(name: String, value: Boolean, subprojectName: String?): Boolean =
        report(BooleanMetrics.valueOf(name), value, subprojectName, null)


    override fun reportNumber(name: String, value: Long, subprojectName: String?): Boolean =
        report(NumericalMetrics.valueOf(name), value, subprojectName, null)


    override fun reportString(name: String, value: String, subprojectName: String?): Boolean =
        report(StringMetrics.valueOf(name), value, subprojectName, null)

}
