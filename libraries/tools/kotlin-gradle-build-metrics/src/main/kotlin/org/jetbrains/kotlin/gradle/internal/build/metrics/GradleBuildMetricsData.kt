/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.build.metrics

import java.io.Serializable

class GradleBuildMetricsData : Serializable {
    val parentMetric: MutableMap<String, String?> = LinkedHashMap()
    val buildAttributeKind: MutableMap<String, String> = LinkedHashMap()
    val buildOperationData: MutableMap<String, BuildOperationData> = LinkedHashMap()

    companion object {
        const val serialVersionUID = 0L
    }
}

/** Data for a build operation (e.g., task or transform). */
data class BuildOperationData(
    val path: String,
    val typeFqName: String,
    val buildTimesMs: Map<String, Long>,
    val performanceMetrics: Map<String, Long>,
    val buildAttributes: Map<String, Int>,
    val didWork: Boolean
) : Serializable {

    companion object {
        const val serialVersionUID = 0L
    }
}