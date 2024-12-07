/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.reports.MetadataPropertyReport
import kotlin.metadata.jvm.KmPackageParts
import kotlin.metadata.jvm.UnstableMetadataApi

@OptIn(UnstableMetadataApi::class)
class PackagePartsMetadataTask(
    private val configuration: CheckerConfiguration,
    private val packagePart1: KmPackageParts,
    private val packagePart2: KmPackageParts,
    private val packagePartsReport: MetadataPropertyReport
): Runnable {
    override fun run() {
        for (checker in configuration.enabledPackagePartsMetadataCheckers) {
            checker.check(packagePart1, packagePart2, packagePartsReport)
        }
    }
}