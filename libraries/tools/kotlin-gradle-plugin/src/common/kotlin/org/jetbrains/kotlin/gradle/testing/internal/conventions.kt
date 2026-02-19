/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.testing.TestTaskReports
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

private val Project.testResultsDir: Provider<Directory>
    get() = project.layout.buildDirectory.dir(TestingBasePlugin.TEST_RESULTS_DIR_NAME)
private val Project.reportsDir: DirectoryProperty
    get() = project.extensions.getByType(ReportingExtension::class.java).baseDirectory

internal val Project.testReportsDir: Provider<Directory>
    get() = reportsDir.dir(TestingBasePlugin.TESTS_DIR_NAME)

internal fun KotlinTest.configureConventions() {
    reports.configureConventions(project, name)

    binaryResultsDirectory
        .convention(project.testResultsDir.map { it.dir("$name/binary") })
        .finalizeValueOnRead()
}

internal fun TestTaskReports.configureConventions(project: Project, name: String) {
    html.outputLocation.convention(project.testReportsDir.map { it.dir(name) }).finalizeValueOnRead()
    junitXml.outputLocation.convention(project.testResultsDir.map { it.dir(name) }).finalizeValueOnRead()
}
