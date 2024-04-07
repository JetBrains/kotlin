/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.reports.ModuleMetadataReport
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.UnstableMetadataApi

@OptIn(UnstableMetadataApi::class)
class ModuleMetadataTask(
    private val configuration: CheckerConfiguration,
    metadata1Bytes: ByteArray,
    metadata2Bytes: ByteArray,
    private val report: ModuleMetadataReport,
) : Runnable {

    private val metadata1 = metadata1Bytes.toKmModule()
    private val metadata2 = metadata2Bytes.toKmModule()


    override fun run() {
        for (checker in configuration.enabledModuleMetadataCheckers) {
            checker.check(metadata1, metadata2, report)
        }

        checkPackageParts()
    }

    private fun checkPackageParts() {
        val packageParts1 = metadata1.packageParts
        val packageParts2 = metadata2.packageParts

        val commonIds = packageParts1.keys.intersect(packageParts2.keys).sorted()
        for (id in commonIds) {
            val packagePart1 = packageParts1[id]!!
            val packagePart2 = packageParts2[id]!!
            val packagePartsReport = report.packagePartsReport(id)
            PackagePartsMetadataTask(configuration, packagePart1, packagePart2, packagePartsReport).run()
        }
    }
}

@OptIn(UnstableMetadataApi::class)
private fun ByteArray.toKmModule() = KotlinModuleMetadata.read(this).kmModule
