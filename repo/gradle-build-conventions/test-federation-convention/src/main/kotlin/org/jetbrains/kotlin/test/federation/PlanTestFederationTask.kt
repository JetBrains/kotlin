/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Resolves the [TestFederationPlan] and prints it to the build log.
 * The plan is written into a file in the project directory for the CI to schedule test subsets for concrete builds.
 * The same table is published as an HTML file to be shown as a report tab.
 */
open class PlanTestFederationTask : DefaultTask() {

    @get:Input
    internal val changedFiles = project.testFederationChangedFiles

    @get:Input
    internal val changedDomains = project.testFederationChangedDomains

    @get:Input
    internal val affectedDomains = project.testFederationAffectedDomains

    @get:Input
    val defaultBranch = project.providers.gradleProperty("isDefaultBranch").map { it.toBoolean() }.orElse(false)

    @OutputFile
    val diffFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.diff.txt"))

    /**
     * @see TestFederationPlan.renderProperties
     */
    @OutputFile
    val propertiesPlanFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.plan.properties"))

    /**
     * @see [TestFederationPlan.renderHtml]
     */
    @OutputFile
    val htmlPlanFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.plan.html"))

    init {
        usesService(project.featureBranchDiffService)
        usesService(project.affectedDomainsService)
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun planTestFederation() {
        diffFile.get().asFile.writeText(changedFiles.get().joinToString(System.lineSeparator()))

        val allDomains = Domain.entries.toSet()
        val isDefaultBranch = defaultBranch.get()
        val changedDomains = if (isDefaultBranch) allDomains else changedDomains.get()
        val affectedDomains = if (isDefaultBranch) allDomains else affectedDomains.get()

        val plan = TestFederationPlan.resolve(
            isDefaultBranch = isDefaultBranch,
            changedDomains = changedDomains,
            affectedDomains = affectedDomains
        )

        logger.quiet(plan.renderAscii())
        propertiesPlanFile.get().asFile.writeText(plan.renderProperties())
        htmlPlanFile.get().asFile.writeText(plan.renderHtml())
    }
}
