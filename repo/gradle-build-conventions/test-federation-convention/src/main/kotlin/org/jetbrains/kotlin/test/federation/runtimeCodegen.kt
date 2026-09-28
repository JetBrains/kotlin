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
 * Generates the domain enum and annotations that select tests when a domain contains changed files.
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
                        |* Requires the annotated tests to run and pass before merging to master when [Domain.${domain.name}] contains changed files.
                        |* The tests still run whenever all tests in their own domain must run.
                        |* Other test filters, including [NightlyTest], still apply.
                        |*
                        |* ### Extra: Contract tests
                        |* Use this annotation for tests that check behavior another domain relies on.
                        |*/
                    """.trimMargin()
                    this += "|@Tag(\"contract:${domain.name}\")"
                    this += "|annotation class MustRunOnChangesIn${domain.name}"
                    this += "|"
                }

                this += "|fun mustRunOnChangesInAnnotationOf(domain: Domain) = when (domain) {"
                for (domain in domains) {
                    this += "|    Domain.${domain.name} -> MustRunOnChangesIn${domain.name}::class"
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
                this += "|    ;"
                this += "|"
                this += "|    companion object"
                this += "|}"
                this += "|"
            }.trimMargin()
        )
    }
}


private operator fun StringBuilder.plusAssign(s: String) {
    this.appendLine(s)
}
