/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.util.GradleVersion
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Path
import java.util.*
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readText

interface GradleBuildScriptInjection : Serializable {
    fun inject(project: Project)
}

private const val marker = "// MARKER: GradleBuildScriptInjections Enabled"

fun GradleProject.enableBuildScriptInjectionsIfNecessary() {
    val injectionClasses = System.getProperty("buildGradleKtsInjectionsClasspath")
        ?: error("Missing required system property 'buildGradleKtsInjectionsClasspath'")
    val escapedInjectionClasses = injectionClasses
        .replace("\\", "\\\\")
        .replace("$", "\\$")

    if (buildGradle.exists()) {
        if (buildGradle.readText().contains(marker)) return
        buildGradle.modify {
            it.insertBlockToBuildScriptAfterImports("""
            $marker
            buildscript {
                println("⚠️ GradleBuildScriptInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                dependencies {
                    classpath(project.files('$escapedInjectionClasses'))
                }
            }
            
        """.trimIndent())
        }
        return
    }

    if (buildGradleKts.exists()) {
        if (buildGradleKts.readText().contains(marker)) return

        buildGradleKts.modify {
            it.insertBlockToBuildScriptAfterPluginsAndImports("""
            $marker
            buildscript {
                println("⚠️ GradleBuildScriptInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                val files = project.files("$escapedInjectionClasses")
                dependencies {
                    classpath(files)
                }
            }

            """.trimIndent())
        }
        return
    }

    error("build.gradle.kts nor build.gradle files not found in Test Project '$projectName'. Please check if it is a valid gradle project")
}

fun invokeBuildScriptInjection(project: Project, serializedInjectionPath: String) {
    java.io.File(serializedInjectionPath).inputStream().use {
        (ObjectInputStream(it).readObject() as GradleBuildScriptInjection).inject(project)
    }
}

@DslMarker
annotation class BuildGradleKtsInjectionScope

@BuildGradleKtsInjectionScope
class GradleBuildScriptInjectionContext(
    val project: Project,
) {
    val java get() = project.extensions.getByName("java") as JavaPluginExtension
    val kotlinMultiplatform get() = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
    val androidLibrary get() = project.extensions.getByName("android") as LibraryExtension
    val publishing get() = project.extensions.getByName("publishing") as PublishingExtension
    val dependencies get() = project.dependencies
}

class BuildScriptAfterEvaluationReturn<T>(
    val gradleProject: TestProject,
    val serializedReturnPath: File,
    val injectionLoadProperty: String,
) {
    /**
     * Return values to the test by serializing the return after the execution. The benefit of serializing after execution is that we can
     * query anything from relevant tasks since they have executed. However, we have to disable CC, so that the returning closure can reach
     * out for configuration entities.
     */
    fun buildAndReturn(evaluationTask: String = "tasks"): T {
        gradleProject.build(
            evaluationTask,
            "-P${injectionLoadProperty}=true",
            buildOptions = gradleProject.buildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
            )
        )
        ObjectInputStream(serializedReturnPath.inputStream()).use {
            @Suppress("UNCHECKED_CAST")
            return it.readObject() as T
        }
    }
}

inline fun <reified T> TestProject.buildScriptReturn(
    crossinline returnFromProject: GradleBuildScriptInjectionContext.() -> T,
): BuildScriptAfterEvaluationReturn<T> {
    enableBuildScriptInjectionsIfNecessary()
    val injectionUuid = UUID.randomUUID().toString()
    val serializedReturnPath = projectPath.resolve("serializedReturnConfiguration${injectionUuid}").toFile()
    val injection = object : GradleBuildScriptInjection {
        override fun inject(project: Project) {
            val serializeOutput = @JvmSerializableLambda {
                val scope = GradleBuildScriptInjectionContext(project)
                val returnedValue = returnFromProject(scope)
                serializedReturnPath.outputStream().use {
                    ObjectOutputStream(it).writeObject(returnedValue)
                }
            }
            if (GradleVersion.current() < GradleVersion.version("8.0")) {
                @Suppress("DEPRECATION")
                project.gradle.buildFinished {
                    serializeOutput()
                }
            } else {
                project.serviceOf<FlowScope>().always(
                    BuildFinishFlowAction::class.java
                ) {
                    it.parameters.onBuildFinish.set(serializeOutput)
                }
            }
        }
    }

    val serializedInjectionPath = projectPath.resolve("serializedInjection${injectionUuid}")
    serializedInjectionPath.toFile().outputStream().use {
        ObjectOutputStream(it).writeObject(injection)
    }

    when {
        buildGradleKts.exists() -> buildGradleKts.appendText(
            whenPropertySpecified(
                injectionUuid,
                injectionLoad(serializedInjectionPath)
            )
        )
        buildGradle.exists() -> buildGradle.appendText(
            whenPropertySpecified(
                injectionUuid,
                injectionLoadGroovy(serializedInjectionPath)
            )
        )
        else -> error("Can't find the build script to append the return injection")
    }

    return BuildScriptAfterEvaluationReturn(
        this,
        serializedReturnPath,
        injectionUuid,
    )
}

/**
 * Inject build script with a lambda that will be executed by the build script at configuration time.
 *
 * The [code] closure is going to be serialized to a file using Java serialization. This allows the instance of the lambda to capture
 * serializable parameters from the test. When the build script executes, it deserializes the lambda instance and executes it.
 */
inline fun GradleProject.buildScriptInjection(
    crossinline code: GradleBuildScriptInjectionContext.() -> Unit,
) {
    enableBuildScriptInjectionsIfNecessary()
    // it is important to create an anonymous object here, so that we can invoke this via reflection in buildscripts
    // because regular lambdas get executed through ivokedynamic logic. i.e. classes created on fly.
    val injection = object : GradleBuildScriptInjection {
        override fun inject(project: Project) {
            val scope = GradleBuildScriptInjectionContext(project)
            scope.code()
        }
    }

    val serializedInjectionPath = projectPath.resolve("serializedConfiguration${UUID.randomUUID()}")
    serializedInjectionPath.toFile().outputStream().use {
        ObjectOutputStream(it).writeObject(injection)
    }

    when {
        buildGradleKts.exists() -> buildGradleKts.appendText(injectionLoad(serializedInjectionPath))
        buildGradle.exists() -> buildGradle.appendText(injectionLoadGroovy(serializedInjectionPath))
        else -> error("Can't find the build script to append the injection")
    }
}

fun TestProject.transferDependencyResolutionRepositoriesIntoProjectRepositories() {
    when {
        settingsGradleKts.exists() -> settingsGradleKts.appendText(copyRepositoriesFromSettingsIntoProject())
        settingsGradle.exists() -> settingsGradle.appendText(copyRepositoriesFromSettingsIntoProjectGroovy())
        else -> error("Can't find the settings file to configure repositories copying")
    }
}

fun whenPropertySpecified(
    property: String,
    execute: String,
): String = """
    
    if (project.hasProperty("${property}")) {
        ${execute}
    }
    
""".trimIndent()

fun injectionLoad(
    serializedInjectionPath: Path,
): String = """
    
    org.jetbrains.kotlin.gradle.testbase.invokeBuildScriptInjection(project, "${serializedInjectionPath.toFile().path}")
    """.trimIndent()

fun injectionLoadGroovy(
    serializedInjectionPath: Path,
): String = """
    
    org.jetbrains.kotlin.gradle.testbase.TestGradleBuildInjectionKt.invokeBuildScriptInjection(project, '$serializedInjectionPath')
""".trimIndent()

fun copyRepositoriesFromSettingsIntoProject(): String = """
    
    gradle.beforeProject {
        dependencyResolutionManagement.repositories.all {
            repositories.add(this)
        }
    }
""".trimIndent()

fun copyRepositoriesFromSettingsIntoProjectGroovy(): String = """
    
    gradle.beforeProject { project ->
        dependencyResolutionManagement.repositories.all { repository ->
            project.repositories.add(repository)
        }
    }
""".trimIndent()

class BuildFinishFlowAction : FlowAction<BuildFinishFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:Input
        val onBuildFinish: Property<() -> Unit>
    }

    override fun execute(parameters: Parameters) {
        parameters.onBuildFinish.get().invoke()
    }
}

