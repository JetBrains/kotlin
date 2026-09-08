/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.testbase.GradleProjectBuildScriptInjectionContext
import org.jetbrains.kotlin.gradle.testbase.GradleProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * A single (producer, consumer configuration) pair to be checked inside one consumer build.
 *
 * Instead of declaring the producer as a dependency of the consumer's source sets and resolving the real
 * configuration, a "probe" configuration is created: it mirrors [mirroredConfiguration] (same attributes, same
 * inherited dependencies) and additionally declares [producerCoordinates], if the producer is visible from that
 * configuration at all. This way many producers can be checked in a single build without them affecting each other.
 */
internal data class ResolutionProbe(
    val producerId: String,
    val producerCoordinates: String?,
    val mirroredConfiguration: String,
) : Serializable {
    /**
     * Deliberately does not end with [mirroredConfiguration]: plugins often react to configuration names by suffix.
     */
    val probeConfigurationName get() = "probe_${mirroredConfiguration}_for_$producerId"

    val reportPath get() = "$producerId/$mirroredConfiguration.txt"
}

fun GradleProject.prepareConsumerProject(
    consumer: Scenario.Project,
    producers: List<Scenario.Project>,
    localRepoDir: Path,
) {
    settingsGradleKts.replaceText("""dependencyResolutionManagement {""", """
        dependencyResolutionManagement {
            repositories {
                maven("${localRepoDir.absolutePathString().replace("\\", "\\\\")}")
            }
                
    """.trimIndent())

    if (consumer.variant is ProjectVariant.Kmp) enableKmpConsumerTargets(consumer)

    val probes = producers.flatMap { producer ->
        consumer.resolvedConfigurationsNames.map { configurationName ->
            ResolutionProbe(
                producerId = producer.id,
                producerCoordinates = "${producer.packageName}:${producer.artifactName}:1.0"
                    .takeIf { producer.isVisibleFrom(configurationName, consumer) },
                mirroredConfiguration = configurationName,
            )
        }
    }

    buildScriptInjection {
        registerResolveDependenciesTask(probes)
    }
}

private fun GradleProject.enableKmpConsumerTargets(consumer: Scenario.Project) {
    val projectVariant = consumer.variant
    check(projectVariant is ProjectVariant.Kmp)
    val kotlinVersion = checkNotNull(consumer.kotlinVersion)

    if (projectVariant.withJvm) {
        buildGradleKts.replaceText("// jvm() // JVM", "jvm() // JVM")
    }

    if (projectVariant.withAndroid) {
        if (kotlinVersion < KotlinToolingVersion("1.9.20")) {
            buildGradleKts.replaceText("androidTarget", "android")
        }
        buildGradleKts.replaceText("// id(\"com.android.library\") // AGP", "id(\"com.android.library\") // AGP")
        buildGradleKts.replaceText("/* Begin AGP", "// /* Begin AGP")
        buildGradleKts.replaceText("End AGP */", "// End AGP */")
    }
}

/**
 * Repeats the source set placement of the producer dependency: a producer that can't be declared in `commonMain` is
 * only visible from the compile classpaths of the targets its own source sets were added to.
 */
private fun Scenario.Project.isVisibleFrom(configurationName: String, consumer: Scenario.Project): Boolean {
    val consumerVariant = consumer.variant
    if (consumerVariant !is ProjectVariant.Kmp) return true
    if (consumerVariant.isCommonMainDependableOn(variant)) return true

    return when (configurationName) {
        "jvmCompileClasspath" -> hasJvm
        "flavor1DebugCompileClasspath", "flavor1ReleaseCompileClasspath" -> hasAndroid
        "linuxX64CompileKlibraries", "linuxArm64CompileKlibraries" -> isKmp
        else -> error("Unexpected resolved configuration name: $configurationName")
    }
}

private abstract class ResolveDependenciesTask : DefaultTask() {
    private class Report(
        val probe: ResolutionProbe,
        val dependencies: Set<DependencyResult>,
        val components: Set<ResolvedComponentResult>,
    )

    @get:OutputDirectory
    val outDir: File = project.file("resolvedDependenciesReports")

    private val reports = mutableListOf<Report>()

    fun reportForProbe(probe: ResolutionProbe) {
        val mirroredConfiguration = project.configurations.findByName(probe.mirroredConfiguration) ?: return
        val probeConfiguration = createProbeConfiguration(probe, mirroredConfiguration)
        reports += probeConfiguration.incoming.resolutionResult.let { result ->
            Report(probe, result.allDependencies, result.allComponents)
        }
    }

    private fun createProbeConfiguration(probe: ResolutionProbe, mirroredConfiguration: Configuration): Configuration =
        project.configurations.create(probe.probeConfigurationName) { probeConfiguration ->
            probeConfiguration.isCanBeConsumed = false
            probeConfiguration.isCanBeResolved = true
            // inherits the dependencies the consumer project declares itself, including the ones added lazily by plugins
            probeConfiguration.extendsFrom(mirroredConfiguration)
            for (attribute in mirroredConfiguration.attributes.keySet()) {
                @Suppress("UNCHECKED_CAST")
                probeConfiguration.attributes.attribute(
                    attribute as Attribute<Any>,
                    mirroredConfiguration.attributes.getAttribute(attribute)!!,
                )
            }
            probe.producerCoordinates?.let {
                probeConfiguration.dependencies.add(project.dependencies.create(it))
            }
        }

    @TaskAction
    fun action() {
        reports.forEach { report -> reportResolutionResult(report) }
    }

    private fun reportResolutionResult(report: Report) {
        // the probe configuration stands in for the real one: report it under the name of the mirrored configuration
        fun String.asMirroredConfiguration() = replace(report.probe.probeConfigurationName, report.probe.mirroredConfiguration)

        val content = buildString {
            // report errors if any
            report.dependencies
                .filterIsInstance<UnresolvedDependencyResult>()
                .forEach {
                    appendLine("ERROR: ${it.attempted} -> ${it.failure}".asMirroredConfiguration())
                }

            report.components
                .map { component -> "${component.id} => ${component.variants.map { it.displayName }}".asMirroredConfiguration() }
                .sorted()
                .joinToString("\n")
                .also { append(it) }
        }

        val reportFile = outDir.resolve(report.probe.reportPath)
        reportFile.parentFile.mkdirs()
        reportFile.writeText(content)
    }
}

internal fun GradleProjectBuildScriptInjectionContext.registerResolveDependenciesTask(probes: List<ResolutionProbe>) {
    project.tasks.register("resolveDependencies", ResolveDependenciesTask::class.java) { task ->
        for (probe in probes) {
            task.reportForProbe(probe)
        }
    }
}
