/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText


/**
 * This task infers the currently affected [Domain]s by using the [featureBranchDiffService] and [affectedDomainsService]
 * The diff and 'affected' subystems are written into files in the project directory.
 *
 * This task will also communicate with TeamCity by setting the [TEST_FEDERATION_AFFECTED_DOMAINS_KEY] parameter.
 * Once this task was executed, all builds on TeamCity in the same chain will be able to use the inferred subsystems.
 */
@Suppress("unused") // declared as task in build.gradle.kts
open class TestFederationInferAffectedDomainsTask : DefaultTask() {

    @get:Internal
    internal val diffService = project.featureBranchDiffService

    @get:Input
    internal val affectedDomains = project.testFederationAffectedDomains

    init {
        usesService(diffService)
        usesService(project.affectedDomainsService)
        outputs.upToDateWhen { false }
    }

    @OutputFile
    val diffFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.diff.txt"))

    @OutputFile
    val affectedDomainsFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.affected-domains.txt"))

    @Input
    val defaultBranch: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(project.providers.gradleProperty("isDefaultBranch").map { it.toBoolean() }.orElse(false))


    @TaskAction
    fun inferAffectedDomains() {
        val diffFile = diffFile.get().asFile.toPath()
        diffFile.parent.createDirectories()
        diffFile.writeText(diffService.get().diff.joinToString(System.lineSeparator()))

        val affectedDomainsFile = this@TestFederationInferAffectedDomainsTask.affectedDomainsFile.get().asFile.toPath()
        val affectedDomains = if (!defaultBranch.get()) this@TestFederationInferAffectedDomainsTask.affectedDomains.get() else {
            logger.quiet("Default branch; All domains are marked as affected")
            Domain.entries.toSet()
        }

        affectedDomainsFile.parent.createDirectories()
        affectedDomainsFile.writeText(affectedDomains.joinToString(System.lineSeparator()))

        /*
        Communicate with TeamCity:
        - Set the TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY parameter
        - Add a build tag for each affected domain
         */
        println("##teamcity[setParameter name='$TEST_FEDERATION_AFFECTED_DOMAINS_KEY' value='${affectedDomains.toArgumentString()}']")
        affectedDomains.forEach { domain ->
            println("##teamcity[addBuildTag 'Affected: $domain']")
        }
    }
}

