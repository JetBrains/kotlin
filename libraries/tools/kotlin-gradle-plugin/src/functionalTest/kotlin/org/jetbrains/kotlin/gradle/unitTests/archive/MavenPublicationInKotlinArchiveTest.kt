/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.archive

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.MavenPublicationIgnoredForTargetInKotlinArchive
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class MavenPublicationInKotlinArchiveTest {
    @Test
    fun `mavenPublication calls on target stored in kotlin archive are reported with their locations`() {
        val callLines = mutableListOf<Int>()
        val project = buildPublishingProject {
            publishing { publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE) }
            linuxX64 {
                callLines.add(currentLine() + 1)
                mavenPublication { }
                callLines.add(currentLine() + 1)
                mavenPublication { }
            }
        }.evaluate()

        val diagnostics = project.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(project.path)
            .filter { it.id == MavenPublicationIgnoredForTargetInKotlinArchive.id }

        assertEquals(
            callLines.map {
                MavenPublicationIgnoredForTargetInKotlinArchive("linuxX64", Throwable()).message to it
            }.prettyPrinted,
            diagnostics.map { diagnostic ->
                val testFrame = diagnostic.throwable!!.stackTrace.first { it.className.startsWith(javaClass.name) }
                diagnostic.message to testFrame.lineNumber
            }.prettyPrinted,
        )
    }

    @Test
    fun `no diagnostic without mavenPublication calls`() {
        val project = buildPublishingProject {
            publishing { publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE) }
            linuxX64()
        }.evaluate()

        project.assertNoDiagnostics(MavenPublicationIgnoredForTargetInKotlinArchive)
    }

    @Test
    fun `no diagnostic for target not stored in kotlin archive`() {
        val project = buildPublishingProject {
            publishing { publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE) }
            linuxX64()
            jvm { mavenPublication { } }
        }.evaluate()

        project.assertNoDiagnostics(MavenPublicationIgnoredForTargetInKotlinArchive)
    }

    @Test
    fun `no diagnostic with default publication format`() {
        val project = buildPublishingProject {
            linuxX64 { mavenPublication { } }
        }.evaluate()

        project.assertNoDiagnostics(MavenPublicationIgnoredForTargetInKotlinArchive)
    }

    private fun buildPublishingProject(configure: KotlinMultiplatformExtension.() -> Unit): ProjectInternal =
        buildProjectWithMPP {
            plugins.apply("maven-publish")
            kotlin(configure)
        }

    private fun currentLine(): Int = Throwable().stackTrace[1].lineNumber
}
