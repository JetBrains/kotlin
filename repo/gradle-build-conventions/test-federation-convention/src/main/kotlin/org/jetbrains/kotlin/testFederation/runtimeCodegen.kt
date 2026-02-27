/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.tooling.core.withClosure
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Generates code available at runtime for tests (e.g. all contract annotations such as '@CompilerContract', ...)
 */

@Suppress("unused")
abstract class GenerateTestFederationRuntimeCodeTask : DefaultTask() {
    @get:InputFile
    val declaredSystems: RegularFileProperty = project.objects.fileProperty()
        .convention(project.isolated.rootProject.projectDirectory.file("repo/subsystems.yaml"))

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("genSrc"))

    @TaskAction
    fun generateCode() {
        val systems = declaredSystems.get().asFile.toPath().readDeclaredSubsystems()

        outputDir.asFile.get().toPath().resolve("annotations.kt").createParentDirectories().writeText(
            buildString {
                this += "|// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "|// See ${GenerateTestFederationRuntimeCodeTask::class.simpleName}"
                this += "|"
                this += "|package org.jetbrains.kotlin.testFederation"
                this += "|import org.junit.jupiter.api.Tag"
                this += "|"

                for (system in systems.withClosure<DeclaredSubsystem> { system -> system.subsystems }) {
                    this += "|@Tag(\"contract:${system.name}\")"
                    this += "|annotation class ${system.name}Contract"
                    this += "|"
                }
            }.trimMargin()
        )

        val allSystems = systems.withClosure<DeclaredSubsystem> { system -> system.subsystems }

        outputDir.asFile.get().toPath().resolve("subsystems.kt").createParentDirectories().writeText(
            buildString {
                this += "|// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "|// See ${GenerateTestFederationRuntimeCodeTask::class.simpleName}"
                this += "|"
                this += "package org.jetbrains.kotlin.testFederation"
                this += "|"
                this += "|enum class Subsystem {"
                for (system in allSystems) {
                    this += "|    ${system.name},"
                }
                this += "|    Unknown,"
                this += "|}"
                this += "|"
            }.trimMargin()
        )

        outputDir.asFile.get().toPath().resolve("keys.kt").createParentDirectories().writeText(
            buildString {
                this += "|// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "|// See ${GenerateTestFederationRuntimeCodeTask::class.simpleName}"
                this += "|"
                this += "|package org.jetbrains.kotlin.testFederation"
                this += "|"
                this += "|const val TEST_FEDERATION_ENABLED_KEY = \"$TEST_FEDERATION_ENABLED_KEY\""
                this += "|const val TEST_FEDERATION_ENABLED_ENV_KEY = \"$TEST_FEDERATION_ENABLED_ENV_KEY\""
                this += "|const val TEST_FEDERATION_MODE_KEY = \"$TEST_FEDERATION_MODE_KEY\""
                this += "|const val TEST_FEDERATION_MODE_ENV_KEY = \"$TEST_FEDERATION_MODE_ENV\""
                this += "|const val TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY = \"$TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY\""
                this += "|const val TEST_FEDERATION_AFFECTED_SUBSYSTEMS_ENV_KEY = \"$TEST_FEDERATION_AFFECTED_SUBSYSTEMS_ENV_KEY\""
            }.trimMargin()
        )
    }
}


private operator fun StringBuilder.plusAssign(s: String) {
    this.appendLine(s)
}
