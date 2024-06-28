/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.reports.SyntheticClassMetadataReport

class SyntheticMetadataTask(
    private val configuration: CheckerConfiguration,
    private val metadata1: KotlinClassMetadata.SyntheticClass,
    private val metadata2: KotlinClassMetadata.SyntheticClass,
    private val report: SyntheticClassMetadataReport,
) : Runnable {
    override fun run() {
        for (checker in configuration.enabledAllSyntheticClassMetadataCheckers) {
            checker.check(metadata1, metadata2, report)
        }

        val lambda1 = metadata1.kmLambda
        val lambda2 = metadata2.kmLambda
        if (lambda1 != null && lambda2 != null) {
            GenericMetadataTask(
                lambda1.function,
                lambda2.function,
                report.functionReport(),
                configuration.enabledFunctionMetadataCheckers
            ).run()
        }
    }
}
