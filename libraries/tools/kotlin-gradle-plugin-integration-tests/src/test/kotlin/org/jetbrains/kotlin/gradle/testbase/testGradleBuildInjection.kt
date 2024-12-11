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
import org.gradle.api.initialization.Settings
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
import kotlin.io.path.*

interface GradleBuildScriptInjection<T> : Serializable {
    fun inject(target: T)
}

private const val buildScriptInjectionsMarker = "// MARKER: GradleBuildScriptInjections Enabled"

fun GradleProject.enableBuildScriptInjectionsIfNecessary(
    buildScript: Path,
    buildScriptKts: Path,
) {
    val injectionClasses = System.getProperty("buildScriptInjectionsClasspath")
        ?: error("Missing required system property '${"buildScriptInjectionsClasspath"}'")
    val escapedInjectionClasses = injectionClasses
        .replace("\\", "\\\\")
        .replace("$", "\\$")

    if (buildScript.exists()) {
        if (buildScript.readText().contains(buildScriptInjectionsMarker)) return
        buildScript.modify {
            it.insertBlockToBuildScriptAfterImports("""
            $buildScriptInjectionsMarker
            buildscript {
                println("⚠️ GradleBuildScriptInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                dependencies {
                    classpath(files('$escapedInjectionClasses'))
                }
            }
            
        """.trimIndent())
        }
        return
    }

    if (buildScriptKts.exists()) {
        if (buildScriptKts.readText().contains(buildScriptInjectionsMarker)) return

        buildScriptKts.modify {
            it.insertBlockToBuildScriptAfterImports("""
            $buildScriptInjectionsMarker
            buildscript {
                println("⚠️ GradleBuildScriptInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                val classes = files("$escapedInjectionClasses")
                dependencies {
                    classpath(classes)
                }
            }

            """.trimIndent())
        }
        return
    }

    error("build.gradle.kts nor build.gradle files not found in Test Project '$projectName'. Please check if it is a valid gradle project")
}

class InjectionLoader {
    fun invokeBuildScriptInjection(project: Project, serializedInjectionName: String) {
        project.projectDir.resolve(serializedInjectionName).inputStream().use {
            @Suppress("UNCHECKED_CAST")
            (ObjectInputStream(it).readObject() as GradleBuildScriptInjection<Project>).inject(project)
        }
    }

    fun invokeSettingsBuildScriptInjection(settings: Settings, serializedInjectionName: String) {
        settings.settingsDir.resolve(serializedInjectionName).inputStream().use {
            @Suppress("UNCHECKED_CAST")
            (ObjectInputStream(it).readObject() as GradleBuildScriptInjection<Settings>).inject(settings)
        }
    }
}

@DslMarker
annotation class BuildGradleKtsInjectionScope

@BuildGradleKtsInjectionScope
class GradleProjectBuildScriptInjectionContext(
    val project: Project,
) {
    val java get() = project.extensions.getByName("java") as JavaPluginExtension
    val kotlinMultiplatform get() = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
    val androidLibrary get() = project.extensions.getByName("android") as LibraryExtension
    val publishing get() = project.extensions.getByName("publishing") as PublishingExtension
    val dependencies get() = project.dependencies
}

@BuildGradleKtsInjectionScope
class GradleSettingsBuildScriptInjectionContext(
    val settings: Settings,
)

class ReturnFromBuildScriptAfterExecution<T>(
    val returnContainingGradleProject: TestProject,
    val serializedReturnPath: File,
    val injectionLoadProperty: String,
) {
    /**
     * Return values to the test by serializing the return after the execution. The benefit of serializing after execution is that we can
     * query anything from relevant tasks since they have executed. However, we have to disable CC, so that the returning closure can reach
     * out for configuration entities.
     */
    fun buildAndReturn(
        evaluationTask: String = "tasks",
        executingProject: TestProject = returnContainingGradleProject,
        configurationCache: BuildOptions.ConfigurationCacheValue = BuildOptions.ConfigurationCacheValue.DISABLED,
        deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
    ): T {
        executingProject.build(
            evaluationTask,
            "-P${injectionLoadProperty}=true",
            buildOptions = executingProject.deriveBuildOptions().copy(
                configurationCache = configurationCache,
            )
        )
        ObjectInputStream(serializedReturnPath.inputStream()).use {
            @Suppress("UNCHECKED_CAST")
            return it.readObject() as T
        }
    }
}

inline fun <reified T> TestProject.buildScriptReturn(
    crossinline returnFromProject: GradleProjectBuildScriptInjectionContext.() -> T,
): ReturnFromBuildScriptAfterExecution<T> {
    enableBuildScriptInjectionsIfNecessary(
        buildGradle,
        buildGradleKts,
    )
    val injectionIdentifier = generateIdentifier()
    val serializedReturnPath = projectPath.resolve("serializedReturnConfiguration_${injectionIdentifier}").toFile()
    val injection = object : GradleBuildScriptInjection<Project> {
        override fun inject(target: Project) {
            val returnEvaluationProvider = target.providers.provider {
                val scope = GradleProjectBuildScriptInjectionContext(target)
                returnFromProject(scope)
            }
            val serializeOutput = {
                val returnValue = returnEvaluationProvider.get()
                serializedReturnPath.outputStream().use {
                    ObjectOutputStream(it).writeObject(returnValue)
                }
            }
            if (GradleVersion.current() < GradleVersion.version("8.0")) {
                @Suppress("DEPRECATION")
                target.gradle.buildFinished {
                    serializeOutput()
                }
            } else {
                target.serviceOf<FlowScope>().always(
                    BuildFinishFlowAction::class.java
                ) {
                    it.parameters.onBuildFinish.set(serializeOutput)
                }
            }
        }
    }

    val serializedInjectionName = "serializedInjection_${injectionIdentifier}"
    val serializedInjectionPath = projectPath.resolve(serializedInjectionName)
    serializedInjectionPath.toFile().outputStream().use {
        ObjectOutputStream(it).writeObject(injection)
    }

    when {
        buildGradleKts.exists() -> buildGradleKts.appendText(
            whenPropertySpecified(
                injectionIdentifier,
                injectionLoadProject(serializedInjectionName)
            )
        )
        buildGradle.exists() -> buildGradle.appendText(
            whenPropertySpecified(
                injectionIdentifier,
                injectionLoadProjectGroovy(serializedInjectionName)
            )
        )
        else -> error("Can't find the build script to append the return injection")
    }

    return ReturnFromBuildScriptAfterExecution(
        this,
        serializedReturnPath,
        injectionIdentifier,
    )
}

/**
 * Inject build script with a lambda that will be executed by the build script at configuration time.
 *
 * The [code] closure is going to be serialized to a file using Java serialization. This allows the instance of the lambda to capture
 * serializable parameters from the test. When the build script executes, it deserializes the lambda instance and executes it.
 */
fun GradleProject.buildScriptInjection(
    code: GradleProjectBuildScriptInjectionContext.() -> Unit,
) {
    enableBuildScriptInjectionsIfNecessary(
        buildGradle,
        buildGradleKts,
    )
    loadInjectionDuringEvaluation<GradleProjectBuildScriptInjectionContext, Project>(
        buildGradle,
        buildGradleKts,
        ::injectionLoadProject,
        ::injectionLoadProjectGroovy,
        { GradleProjectBuildScriptInjectionContext(it) },
        code,
    )
}

fun GradleProject.settingsBuildScriptInjection(
    code: GradleSettingsBuildScriptInjectionContext.() -> Unit,
) {
    loadInjectionDuringEvaluation<GradleSettingsBuildScriptInjectionContext, Settings>(
        settingsGradle,
        settingsGradleKts,
        ::injectionLoadSettings,
        ::injectionLoadSettingsGroovy,
        { GradleSettingsBuildScriptInjectionContext(it) },
        code,
    )
}

fun <Context, Target> GradleProject.loadInjectionDuringEvaluation(
    buildScript: Path,
    buildScriptKts: Path,
    injectionLoad: (String) -> String,
    injectionLoadGroovy: (String) -> String,
    instantiateInjectionContext: (Target) -> Context,
    code: Context.() -> Unit,
) {
    // it is important to create an anonymous object here, so that we can invoke this via reflection in buildscripts
    // because regular lambdas get executed through ivokedynamic logic. i.e. classes created on fly.
    val injection = object : GradleBuildScriptInjection<Target> {
        override fun inject(target: Target) {
            val context = instantiateInjectionContext(target)
            context.code()
        }
    }

    val serializedInjectionName = "serializedConfiguration_${UUID.randomUUID()}"
    val serializedInjectionPath = projectPath.resolve(serializedInjectionName)
    serializedInjectionPath.toFile().outputStream().use {
        ObjectOutputStream(it).writeObject(injection)
    }

    when {
        buildScriptKts.exists() -> buildScriptKts.appendText(injectionLoad(serializedInjectionName))
        buildScript.exists() -> buildScript.appendText(injectionLoadGroovy(serializedInjectionName))
        else -> error("Can't find the build script to append the injection")
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

/**
 * Settings injections must use [scriptIsolatedInjectionLoad] instead of the [enableBuildScriptInjectionsIfNecessary] mechanism. This is
 * because [GradleProjectBuildScriptInjectionContext] must be loaded by the build script's classloader to
 *  - be able to see KGP classes loaded by the build script classloader
 *  - be visible to the execution-time classloader in runs with CC
 *
 * Settings injections only run at configuration time and must therefore prevent Gradle from loading any classes into the settings
 * classloader. Gradle disposes of the settings classloader before execution and complains if the project build script referenced anything
 * captured for execution from this classloader
 */
fun scriptIsolatedInjectionLoad(
    targetMethodName: String,
    targetPropertyName: String,
    targetPropertyClassName: String,
    serializedInjectionName: String,
): String {
    val injectionClasses = System.getProperty("buildScriptInjectionsClasspath")
        ?: error("Missing test classes output directory in property '${"buildScriptInjectionsClasspath"}'")
    val escapedInjectionClasses = injectionClasses
        .replace("\\", "\\\\")
        .replace("$", "\\$")
    val lambdaName = "invokeInjection${serializedInjectionName.replace("-", "_")}"

    return """
        
        val $lambdaName = {
            val testClasses = arrayOf(File("$escapedInjectionClasses").toURI().toURL())
            val injectionLoaderClass = java.net.URLClassLoader(
                testClasses, 
                this.javaClass.classLoader
            ).loadClass("${InjectionLoader::class.java.name}")
            injectionLoaderClass.getMethod(
                "$targetMethodName",
                Class.forName("$targetPropertyClassName"),
                Class.forName("java.lang.String")
            ).invoke(
                injectionLoaderClass.getConstructor().newInstance(), 
                ${targetPropertyName},
                "$serializedInjectionName"
            )
        }
        ${lambdaName}()
    """.trimIndent()
}

fun scriptIsolatedInjectionLoadGroovy(
    targetMethodName: String,
    targetPropertyName: String,
    targetPropertyClassName: String,
    serializedInjectionName: String,
): String {
    val injectionClasses = System.getProperty("buildScriptInjectionsClasspath")
        ?: error("Missing test classes output directory in property '${"buildScriptInjectionsClasspath"}'")
    val escapedInjectionClasses = injectionClasses
        .replace("\\", "\\\\")
        .replace("$", "\\$")
    val lambdaName = "invokeInjection${serializedInjectionName.replace("-", "_")}"

    return """
        
        def ${lambdaName} = {
            URL[] testClasses = [new File('${escapedInjectionClasses}').toURI().toURL()]
            def injectionLoaderClass = new URLClassLoader(
                testClasses, 
                this.getClass().classLoader
            ).loadClass('${InjectionLoader::class.java.name}')
            injectionLoaderClass.getMethod(
                '${targetMethodName}',
                Class.forName('${targetPropertyClassName}'),
                Class.forName('java.lang.String')
            ).invoke(
                injectionLoaderClass.getConstructor().newInstance(), 
                ${targetPropertyName},
                '${serializedInjectionName}'
            )
        }
        ${lambdaName}()
    """.trimIndent()
}

fun injectionLoadSettings(
    serializedInjectionName: String,
): String = scriptIsolatedInjectionLoad(
    "invokeSettingsBuildScriptInjection",
    "settings",
    Settings::class.java.name,
    serializedInjectionName,
)

fun injectionLoadSettingsGroovy(
    serializedInjectionName: String,
): String = scriptIsolatedInjectionLoadGroovy(
    "invokeSettingsBuildScriptInjection",
    "settings",
    Settings::class.java.name,
    serializedInjectionName,
)

fun injectionLoadProject(
    serializedInjectionName: String,
): String = """
    
    org.jetbrains.kotlin.gradle.testbase.InjectionLoader().invokeBuildScriptInjection(project, "$serializedInjectionName")
""".trimIndent()

fun injectionLoadProjectGroovy(
    serializedInjectionName: String,
): String = """
    
    new org.jetbrains.kotlin.gradle.testbase.InjectionLoader().invokeBuildScriptInjection(project, '$serializedInjectionName')
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

