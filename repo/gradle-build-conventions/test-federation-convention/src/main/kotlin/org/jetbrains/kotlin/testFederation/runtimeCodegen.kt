/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/**
 * Generates code available at runtime for tests (e.g. all contract annotations such as '@CompilerContract', ...)
 */

@Suppress("unused")
abstract class GenerateTestFederationRuntimeCodeTask : DefaultTask() {

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.projectDirectory.dir("src/main/generated"))

    @TaskAction
    fun generateCode() {
        val domains = Domain.entries

        outputDir.asFile.get().toPath().resolve("annotations.kt").createParentDirectories().writeText(
            buildString {
                this += "|// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "|// See ${GenerateTestFederationRuntimeCodeTask::class.simpleName}"
                this += "|"
                this += "|package org.jetbrains.kotlin.testFederation"
                this += "|import org.junit.jupiter.api.Tag"
                this += "|"

                for (domain in domains) {
                    this += """
                        |/**
                        |* Will mark tests as 'affected by' the given domain [Domain.${domain.name}].
                        |* Such tests will run, additionally, for all commits affecting the ${domain.name} domain.
                        |*/
                    """.trimMargin()
                    this += "|@Tag(\"affectedBy:${domain.name}\")"
                    this += "|annotation class AffectedBy${domain.name}"
                    this += "|"
                }

                this += "|fun affectedByAnnotationOf(domain: Domain) = when (domain) {"
                for (domain in domains) {
                    this += "|    Domain.${domain.name} -> AffectedBy${domain.name}::class"
                }
                this += "|}"
            }.trimMargin()
        )

        outputDir.asFile.get().toPath().resolve("domains.kt").createParentDirectories().writeText(
            buildString {
                this += "|// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "|// See ${GenerateTestFederationRuntimeCodeTask::class.simpleName}"
                this += "|"
                this += "package org.jetbrains.kotlin.testFederation"
                this += "|"
                this += "|enum class Domain {"
                for (domain in domains) {
                    this += "|    ${domain.name},"
                }
                this += "|"
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
                this += "|const val TEST_FEDERATION_AFFECTED_DOMAINS_KEY = \"$TEST_FEDERATION_AFFECTED_DOMAINS_KEY\""
                this += "|const val TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY = \"$TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY\""
            }.trimMargin()
        )
    }
}


private operator fun StringBuilder.plusAssign(s: String) {
    this.appendLine(s)
}
