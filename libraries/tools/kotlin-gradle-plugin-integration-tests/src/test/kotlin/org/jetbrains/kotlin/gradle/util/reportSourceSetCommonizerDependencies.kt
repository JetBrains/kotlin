/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.testkit.runner.BuildResult
import org.intellij.lang.annotations.Language
import org.intellij.lang.annotations.RegExp
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.commonizerTarget
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.tooling.core.linearClosure
import java.io.File
import java.nio.file.Path
import javax.annotation.RegEx
import kotlin.io.path.appendText
import kotlin.test.fail

data class SourceSetCommonizerDependency(
    val sourceSetName: String,
    val target: CommonizerTarget,
    val file: File,
)

data class SourceSetCommonizerDependencies(
    val sourceSetName: String,
    val dependencies: Set<SourceSetCommonizerDependency>,
) {

    fun withoutNativeDistributionDependencies(konanDataDirProperty: Path): SourceSetCommonizerDependencies {
        return SourceSetCommonizerDependencies(
            sourceSetName,
            dependencies.filter { dependency -> !dependency.isFromNativeDistribution(konanDataDirProperty) }.toSet()
        )
    }

    fun onlyNativeDistributionDependencies(konanDataDirProperty: Path): SourceSetCommonizerDependencies {
        return SourceSetCommonizerDependencies(
            sourceSetName,
            dependencies.filter { dependency -> dependency.isFromNativeDistribution(konanDataDirProperty) }.toSet()
        )
    }

    private fun SourceSetCommonizerDependency.isFromNativeDistribution(konanDataDirProperty: Path?): Boolean {
        val konanDataDir = konanDataDirProperty?.toRealPath()?.toFile() ?: System.getenv("KONAN_DATA_DIR")?.let(::File)
        if (konanDataDir != null) {
            return file.startsWith(konanDataDir)
        }
        return file.parentsClosure.any { parentFile -> parentFile.name == ".konan" }
    }

    fun assertTargetOnAllDependencies(target: CommonizerTarget) = apply {
        dependencies.forEach { dependency ->
            if (dependency.target != target) {
                fail("$sourceSetName: Expected target $target but found dependency with target ${dependency.target}\n$dependency")
            }
        }
    }

    fun assertEmpty() = apply {
        if (dependencies.isNotEmpty()) {
            fail("$sourceSetName: Expected no dependencies in set. Found $dependencies")
        }
    }

    fun assertNotEmpty() = apply {
        if (dependencies.isEmpty()) {
            fail("$sourceSetName: Missing dependencies")
        }
    }

    fun assertDependencyFilesMatches(@Language("RegExp") @RegEx @RegExp vararg fileMatchers: String?) = apply {
        assertDependencyFilesMatches(fileMatchers.filterNotNull().map(::Regex).toSet())
    }

    fun assertDependencyFilesMatches(fileMatchers: Set<Regex>) = apply {
        val unmatchedDependencies = dependencies.filter { dependency ->
            fileMatchers.none { matcher -> dependency.file.absolutePath.matches(matcher) }
        }

        val unmatchedMatchers = fileMatchers.filter { matcher ->
            dependencies.none { dependency -> dependency.file.absolutePath.matches(matcher) }
        }

        if (unmatchedDependencies.isNotEmpty() || unmatchedMatchers.isNotEmpty()) {
            fail(buildString {
                fun appendLineIndented(value: Any?) = appendLine(value.toString().prependIndent("    "))

                appendLine("$sourceSetName: Set of commonizer dependencies does not match given 'fileMatchers'")
                if (unmatchedDependencies.isNotEmpty()) {
                    appendLine("Unmatched dependencies:")
                    unmatchedDependencies.forEach { dependency ->
                        appendLineIndented(dependency)
                    }
                }
                if (unmatchedMatchers.isNotEmpty()) {
                    appendLine("Unmatched fileMatchers:")
                    unmatchedMatchers.forEach { matcher ->
                        appendLineIndented(matcher)
                    }
                }
            })
        }
    }
}

fun interface WithSourceSetCommonizerDependencies {
    fun getCommonizerDependencies(sourceSetName: String): SourceSetCommonizerDependencies
}

fun TestProject.reportSourceSetCommonizerDependencies(
    subproject: String? = null,
    options: BuildOptions = this.buildOptions,
    test: WithSourceSetCommonizerDependencies.(compiledProject: BuildResult) -> Unit,
) {
    if (subproject != null) {
        subProject(subproject).buildGradleKts.appendText(taskSourceCode)
    } else {
        buildGradleKts.appendText(taskSourceCode)
    }

    val taskName = buildString {
        if (subproject != null) append(":$subproject")
        append(":reportCommonizerSourceSetDependencies")
    }

    build(taskName, buildOptions = options) {

        val dependencyReports = output.lineSequence().filter { line -> line.contains("SourceSetCommonizerDependencyReport") }.toList()

        val withSourceSetCommonizerDependencies = WithSourceSetCommonizerDependencies { sourceSetName ->
            val reportMarker = "Report[$sourceSetName]"

            val reportForSourceSet = dependencyReports.firstOrNull { line -> line.contains(reportMarker) }
                ?: fail("Missing dependency report for $sourceSetName")

            val files = reportForSourceSet.split(reportMarker, limit = 2).last().split("|#+#|")
                .map(String::trim).filter(String::isNotEmpty).map(::File)

            val dependencies = files.mapNotNull { file -> createSourceSetCommonizerDependencyOrNull(sourceSetName, file) }.toSet()
            SourceSetCommonizerDependencies(sourceSetName, dependencies)
        }

        withSourceSetCommonizerDependencies.test(this)
    }
}

private fun createSourceSetCommonizerDependencyOrNull(sourceSetName: String, libraryFile: File): SourceSetCommonizerDependency? {
    return SourceSetCommonizerDependency(
        sourceSetName,
        file = libraryFile,
        target = inferCommonizerTargetOrNull(libraryFile) as? SharedCommonizerTarget ?: return null,
    )
}

private fun inferCommonizerTargetOrNull(libraryFile: File): CommonizerTarget? = resolveSingleFileKlib(
    libraryFile = org.jetbrains.kotlin.konan.file.File(libraryFile.path),
    strategy = ToolingSingleFileKlibResolveStrategy
).commonizerTarget?.let(::parseCommonizerTarget)

private val File.parentsClosure: Set<File> get() = this.linearClosure { it.parentFile }

private const val dollar = "\$"

private val taskSourceCode = """

tasks.register("reportCommonizerSourceSetDependencies") {
    kotlin.sourceSets.withType(org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet::class).all {
        inputs.files(configurations.getByName(intransitiveMetadataConfigurationName))
    }

    doLast {
        kotlin.sourceSets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet>().forEach { sourceSet ->
            val configuration = configurations.getByName(sourceSet.intransitiveMetadataConfigurationName)
            val dependencies = configuration.files

            logger.quiet(
                "SourceSetCommonizerDependencyReport[$dollar{sourceSet.name}]$dollar{dependencies.joinToString("|#+#|")}"
            )
        }
    }
}

""".trimIndent()
