/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.collections.joinToString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * This task infers the currently affected [Subsystem]s by using the [featureBranchDiffService] and [affectedTestSystemsService]
 * The diff and 'affected' subystems are written into files in the project directory.
 *
 * This task will also communicate with TeamCity by setting the [TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY] parameter.
 * Once this task was executed, all builds on TeamCity in the same chain will be able to use the inferred subsystems.
 */
open class TestFederationInferAffectedSubsystemsTask : DefaultTask() {

    @get:Internal
    internal val diffService = project.featureBranchDiffService

    @get:Internal
    internal val affectedTestSystemsService = project.affectedSubsystemsService

    init {
        usesService(diffService)
        usesService(affectedTestSystemsService)
        outputs.upToDateWhen { false }
    }

    @OutputFile
    val diffFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.diff.txt"))

    @OutputFile
    val affectedSystemsFile: RegularFileProperty = project.objects.fileProperty()
        .value(project.layout.projectDirectory.file(".test-federation.affected-systems.txt"))

    @TaskAction
    fun inferAffectedSubsystems() {
        val diffFile = diffFile.get().asFile.toPath()
        if (diffFile.exists()) {
            throw IllegalStateException("${diffFile.name} already exists")
        }
        diffFile.parent.createDirectories()
        diffFile.writeText(diffService.get().diff().joinToString(System.lineSeparator()))

        val affectedSystemsFile = affectedSystemsFile.get().asFile.toPath()
        if (affectedSystemsFile.exists()) {
            throw IllegalStateException("${affectedSystemsFile.name} already exists")
        }

        val affectedTestSystems = affectedTestSystemsService.get().affectedSubsystems
        affectedSystemsFile.parent.createDirectories()
        affectedSystemsFile.writeText(affectedTestSystems.joinToString(System.lineSeparator()))

        /*
        Communicate with TeamCity:
        - Set the TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY parameter
        - Add a build tag for each affected subsystem
         */
        println("##teamcity[setParameter name='$TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY' value='${affectedTestSystems.joinToString(";")}']")
        affectedTestSystems.forEach { testSystem ->
            println("##teamcity[addBuildTag 'Affected: $testSystem']")
        }
    }
}

