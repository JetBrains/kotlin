/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleProjectBuildScriptInjectionContext
import org.jetbrains.kotlin.gradle.testbase.GradleProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun GradleProject.prepareConsumerProject(
    consumer: Scenario.Project,
    dependencies: List<Scenario.Project>,
    localRepoDir: Path,
) {
    // prepend with local repository prevent unnecessary network calls
    if (consumer.gradleVersion < GradleVersion.version("8.1")) {
        buildGradleKts.writeText(
            """
        repositories {
            maven("${localRepoDir.absolutePathString().replace("\\", "\\\\")}")
        }

    """.trimIndent() + buildGradleKts.readText()
        )
    } else {
        settingsGradleKts.replaceText("""dependencyResolutionManagement {""", """
            dependencyResolutionManagement {
                repositories {
                    maven("${localRepoDir.absolutePathString().replace("\\", "\\\\")}")
                }
            
        """.trimIndent())
    }

    when (consumer.variant) {
        ProjectVariant.AndroidOnly -> prepareAndroidConsumer(dependencies)
        ProjectVariant.JavaOnly -> prepareJavaConsumer(dependencies)
        is ProjectVariant.Kmp -> prepareKmpConsumer(consumer, dependencies)
    }
}

private fun GradleProject.prepareAndroidConsumer(dependencies: List<Scenario.Project>) {
    buildGradleKts.appendText(
        """
            
            dependencies {
            ${dependencies.asDependenciesBlock()}
            }
        """.trimIndent()
    )

    buildScriptInjection {
        registerResolveDependenciesTask(
            "flavor1DebugCompileClasspath",
            "flavor1ReleaseCompileClasspath"
        )
    }
}

private fun List<Scenario.Project>.asDependenciesBlock(): String = joinToString("\n") {
    """   api("${it.packageName}:${it.artifactName}:1.0") """
}

private fun GradleProject.prepareJavaConsumer(dependencies: List<Scenario.Project>) {
    buildGradleKts.appendText(
        """
            
            dependencies {
            ${dependencies.asDependenciesBlock()}
            }
        """.trimIndent()
    )

    buildScriptInjection {
        registerResolveDependenciesTask("compileClasspath")
    }
}

private fun GradleProject.prepareKmpConsumer(consumer: Scenario.Project, dependencies: List<Scenario.Project>) {
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

    val (commonMainDependencies, targetSpecificDependencies) = dependencies.partition { projectVariant.isCommonMainDependableOn(it.variant) }

    fun List<Scenario.Project>.asSourceSetDependenciesBlock(sourceSetName: String) = """

        sourceSets.getByName("$sourceSetName").dependencies {
        ${this.asDependenciesBlock()}
        }
    """.trimIndent()

    val targetSpecificDependenciesBlock = buildString {
        if (projectVariant.withJvm) {
            appendLine(targetSpecificDependencies.filter { it.hasJvm }.asSourceSetDependenciesBlock("jvmMain"))
        }
        if (projectVariant.withAndroid) {
            appendLine(targetSpecificDependencies.filter { it.hasAndroid }.asSourceSetDependenciesBlock("androidMain"))
        }

        val deps = targetSpecificDependencies.filter { it.isKmp }
        listOf("linuxX64Main", "linuxArm64Main").forEach { appendLine(deps.asSourceSetDependenciesBlock(it)) }
    }

    buildGradleKts.appendText(
        """
            
            kotlin { 
              sourceSets.getByName("commonMain").dependencies {
               ${commonMainDependencies.asDependenciesBlock()}
              }
              $targetSpecificDependenciesBlock
            }           
        """.trimIndent()
    )

    buildScriptInjection {
        registerResolveDependenciesTask(
            "jvmCompileClasspath",
            "androidFlavor1ReleaseCompileClasspath",
            "androidFlavor1DebugCompileClasspath",
            "linuxX64CompileKlibraries",
            "linuxArm64CompileKlibraries"
        )
    }
}


private abstract class ResolveDependenciesTask : DefaultTask() {
    @get:OutputDirectory
    val outDir: File = project.file("resolvedDependenciesReports")

    private val configurations = mutableMapOf<String, ResolutionResult>()
    fun reportForConfiguration(name: String) {
        val configuration = project.configurations.findByName(name) ?: return
        configurations[name] = configuration.incoming.resolutionResult
    }

    @TaskAction
    fun action() {
        configurations.forEach { (name, artifacts) ->
            reportResolutionResult(name, artifacts)
        }
    }

    private fun reportResolutionResult(name: String, resolutionResult: ResolutionResult) {
        val content = buildString {
            // report errors if any
            resolutionResult.allDependencies
                .filterIsInstance<UnresolvedDependencyResult>()
                .forEach {
                    appendLine("ERROR: ${it.attempted} -> ${it.failure}")
                }

            resolutionResult.allComponents
                .map { component -> "${component.id} => ${component.variants.map { it.displayName }}" }
                .sorted()
                .joinToString("\n")
                .also { append(it) }
        }

        outDir.resolve("${name}.txt").writeText(content)
    }
}

internal fun GradleProjectBuildScriptInjectionContext.registerResolveDependenciesTask(vararg configurationNames: String) {
    project.tasks.register("resolveDependencies", ResolveDependenciesTask::class.java) { task ->
        for (configurationName in configurationNames) {
            task.reportForConfiguration(configurationName)
        }
    }
}