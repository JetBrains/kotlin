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
 * Resolves the [TestFederationModel] and prints it to the build log.
 * The model is written into a file in the project directory for the CI to request test subsets for concrete builds.
 * The same table is published as an HTML file to be shown as a report tab.
 */
@Suppress("unused") // declared as task in build.gradle.kts
open class TestFederationResolveModelTask : DefaultTask() {

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
     * @see TestFederationModel.renderTsv
     */
    @OutputFile
    val modelFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.model.tsv"))

    /**
     * @see [TestFederationModel.renderHtml]
     */
    @OutputFile
    val htmlFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.model.html"))

    init {
        usesService(project.featureBranchDiffService)
        usesService(project.affectedDomainsService)
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun resolveModel() {
        diffFile.get().asFile.writeText(changedFiles.get().joinToString(System.lineSeparator()))

        val allDomains = Domain.entries.toSet()
        val isDefaultBranch = defaultBranch.get()
        val changedDomains = if (isDefaultBranch) allDomains else changedDomains.get()
        val affectedDomains = if (isDefaultBranch) allDomains else affectedDomains.get()

        val model = TestFederationModel.compute(
            isDefaultBranch = isDefaultBranch,
            changedDomains = changedDomains,
            affectedDomains = affectedDomains
        )

        logger.quiet(model.renderAscii())
        modelFile.get().asFile.writeText(model.renderTsv())
        htmlFile.get().asFile.writeText(model.renderHtml())

        // Publish the old parameters for backward compatibility (to be removed in favor of test subsets)
        println("##teamcity[setParameter name='$TEST_FEDERATION_AFFECTED_DOMAINS_KEY' value='${affectedDomains.toArgumentString()}']")
        println("##teamcity[setParameter name='$TEST_FEDERATION_CHANGED_DOMAINS_KEY' value='${changedDomains.toArgumentString()}']")
    }
}
