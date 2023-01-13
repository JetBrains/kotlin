/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import java.io.File

internal class KotlinTestReportCompatibilityHelperG6(
    private val objectFactory: ObjectFactory
) : KotlinTestReportCompatibilityHelper {
    override fun getDestinationDirectory(kotlinTestReport: KotlinTestReport): DirectoryProperty =
        objectFactory.directoryProperty().fileValue(kotlinTestReport.destinationDir)

    override fun setDestinationDirectory(kotlinTestReport: KotlinTestReport, directory: File) {
        kotlinTestReport.destinationDir = directory
    }

    override fun addTestResultsFrom(kotlinTestReport: KotlinTestReport, task: AbstractTestTask) {
        kotlinTestReport.reportOn(task.binaryResultsDirectory)
    }

    internal class KotlinTestReportCompatibilityHelperVariantFactoryG6 :
        KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory {
        override fun getInstance(objectFactory: ObjectFactory): KotlinTestReportCompatibilityHelper =
            KotlinTestReportCompatibilityHelperG6(objectFactory)
    }
}