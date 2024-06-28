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
import org.gradle.api.tasks.testing.TestReport
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport

/**
 * Handles the differences in the [TestReport] API introduced in Gradle 7.4
 * See the [migration guide](https://docs.gradle.org/7.6/userguide/upgrading_version_7.html#replacement_methods_in_org_gradle_api_tasks_testing_testreport).
 */
interface KotlinTestReportCompatibilityHelper {
    fun getDestinationDirectory(kotlinTestReport: KotlinTestReport): DirectoryProperty

    fun setDestinationDirectory(kotlinTestReport: KotlinTestReport, directory: Provider<Directory>)

    fun addTestResultsFrom(kotlinTestReport: KotlinTestReport, task: AbstractTestTask)

    interface KotlinTestReportCompatibilityHelperVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(objectFactory: ObjectFactory): KotlinTestReportCompatibilityHelper
    }
}

internal class DefaultKotlinTestReportCompatibilityHelperVariantFactory :
    KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory {
    override fun getInstance(objectFactory: ObjectFactory): KotlinTestReportCompatibilityHelper = DefaultKotlinTestReportCompatibilityHelper()
}

internal class DefaultKotlinTestReportCompatibilityHelper : KotlinTestReportCompatibilityHelper {
    override fun getDestinationDirectory(kotlinTestReport: KotlinTestReport): DirectoryProperty = kotlinTestReport.destinationDirectory

    override fun setDestinationDirectory(kotlinTestReport: KotlinTestReport, directory: Provider<Directory>) {
        kotlinTestReport.destinationDirectory.value(directory).finalizeValueOnRead()
    }

    override fun addTestResultsFrom(kotlinTestReport: KotlinTestReport, task: AbstractTestTask) {
        kotlinTestReport.testResults.from(task.binaryResultsDirectory)
    }
}