/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.reports.MultiFileClassFacadeMetadataReport

class MultiFileClassFacadeMetadataTask(
    private val configuration: CheckerConfiguration,
    private val metadata1: KotlinClassMetadata.MultiFileClassFacade,
    private val metadata2: KotlinClassMetadata.MultiFileClassFacade,
    private val report: MultiFileClassFacadeMetadataReport,
) : Runnable {
    override fun run() {
        for (checker in configuration.enabledMultifileClassFacadeMetadataCheckers) {
            checker.check(metadata1, metadata2, report)
        }
    }
}
