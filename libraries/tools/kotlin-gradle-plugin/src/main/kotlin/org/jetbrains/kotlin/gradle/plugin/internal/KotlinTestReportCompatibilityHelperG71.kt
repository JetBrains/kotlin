/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport

internal class KotlinTestReportCompatibilityHelperG71(
    private val objectFactory: ObjectFactory
) : KotlinTestReportCompatibilityHelper {
    override fun getDestinationDirectory(kotlinTestReport: KotlinTestReport): DirectoryProperty =
        objectFactory.directoryProperty().fileValue(kotlinTestReport.destinationDir)

    override fun setDestinationDirectory(kotlinTestReport: KotlinTestReport, directory: Provider<Directory>) {
        kotlinTestReport.destinationDir = directory.get().asFile
    }

    override fun addTestResultsFrom(kotlinTestReport: KotlinTestReport, task: AbstractTestTask) {
        kotlinTestReport.reportOn(task.binaryResultsDirectory)
    }

    internal class KotlinTestReportCompatibilityHelperVariantFactoryG71 :
        KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory {
        override fun getInstance(objectFactory: ObjectFactory): KotlinTestReportCompatibilityHelper =
            KotlinTestReportCompatibilityHelperG71(objectFactory)
    }
}