/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.flow.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.internal.exceptions.MultiCauseException
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.util.GradleVersion
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull

interface GradleBuildScriptInjection<T> : Serializable {
    fun inject(target: T)
}

/**
 * This injection executes as soon as it is injected with its target
 */
class UndispatchedInjection<Context, Target>(
    val instantiateInjectionContext: (Target) -> Context,
    val executeInjection: Context.() -> Unit
) : GradleBuildScriptInjection<Target> {
    override fun inject(target: Target) = instantiateInjectionContext(target).executeInjection()
}

/**
 * Serializes returned value on build completion that wraps the returned value in a Provider for CC safety.
 *
 * The injection in general executes at:
 * - Provider serialization time with CC
 * - Build completion without CC
 */
class OnBuildCompletionSerializingInjection<Return>(
    val serializedReturnPath: File,
    val returnValueInjection: GradleProjectBuildScriptInjectionContext.() -> Provider<Return>,
) : GradleBuildScriptInjection<Project> {
    class ExecuteOnBuildFinish : FlowAction<ExecuteOnBuildFinish.Parameters> {
        interface Parameters : FlowParameters {
            @get:Input
            val onBuildFinish: Property<() -> Unit>
        }

        override fun execute(parameters: Parameters) {
            parameters.onBuildFinish.get().invoke()
        }
    }

    override fun inject(target: Project) {
        val returnEvaluationProvider = GradleProjectBuildScriptInjectionContext(target).returnValueInjection()
        val serializeOutput = {
            val returnValue = returnEvaluationProvider.get()
            serializedReturnPath.outputStream().use {
                ObjectOutputStream(it).writeObject(returnValue)
            }
        }
        if (GradleVersion.current() < GradleVersion.version("8.1")) {
            @Suppress("DEPRECATION")
            target.gradle.buildFinished {
                serializeOutput()
            }
        } else {
            target.serviceOf<FlowScope>().always(
                ExecuteOnBuildFinish::class.java
            ) {
                it.parameters.onBuildFinish.set(serializeOutput)
            }
        }
    }
}

/**
 * Serializes build failure as [CaughtBuildFailure] (or [CaughtBuildFailure.UnexpectedMissingBuildFailure] is there was no failure) upon build completion
 */
class FindMatchingBuildFailureInjection<ExpectedException : Exception>(
    val serializedReturnPath: File,
    val expectedExceptionClass: Class<ExpectedException>,
) : GradleBuildScriptInjection<Project> {
    class CatchBuildFailure : FlowAction<CatchBuildFailure.Parameters> {
        interface Parameters : FlowParameters {
            @get:Input
            val onBuildFinish: Property<(Throwable?) -> Unit>
            @get:Input
            val buildWorkResult: Property<BuildWorkResult>
        }

        override fun execute(parameters: Parameters) {
            parameters.onBuildFinish.get().invoke(
                parameters.buildWorkResult.get().failure.getOrNull()
            )
        }
    }

    override fun inject(target: Project) {
        val serializeOutput: (Throwable?) -> Unit = { topLevelException ->
            val toSerialize = if (topLevelException == null) {
                CaughtBuildFailure.UnexpectedMissingBuildFailure()
            } else {
                val matchingExceptions = findMatchingExceptions(
                    topLevelException,
                    expectedExceptionClass
                )
                if (matchingExceptions.isNotEmpty()) {
                    CaughtBuildFailure.Expected(matchingExceptions)
                } else {
                    CaughtBuildFailure.Unexpected(
                        java.io.StringWriter().use {
                            PrintWriter(it).use {
                                topLevelException.printStackTrace(it)
                            }
                            it
                        }.toString()
                    )
                }
            }

            serializedReturnPath.outputStream().use {
                ObjectOutputStream(it).writeObject(toSerialize)
            }
        }

        // Catch the errors caused directly by the build failure
        if (GradleVersion.current() < GradleVersion.version("8.0")) {
            @Suppress("DEPRECATION")
            target.gradle.buildFinished {
                serializeOutput(it.failure)
            }
        } else {
            val result = target.serviceOf<FlowProviders>().buildWorkResult
            target.serviceOf<FlowScope>().always(CatchBuildFailure::class.java) {
                it.parameters.onBuildFinish.set(serializeOutput)
                it.parameters.buildWorkResult.set(result)
            }
        }
    }

    private fun <T : Throwable> findMatchingExceptions(
        topLevelException: Throwable,
        targetClass: Class<T>,
    ): Set<T> {
        val exceptionsStack = mutableListOf(topLevelException)
        val walkedExceptions = mutableSetOf<Throwable>()
        val matchingExceptions = mutableSetOf<T>()
        while (exceptionsStack.isNotEmpty()) {
            val current = exceptionsStack.removeLast()
            if (current in walkedExceptions) continue
            walkedExceptions.add(current)
            if (targetClass.isInstance(current)) {
                @Suppress("UNCHECKED_CAST")
                matchingExceptions.add(current as T)
            }
            exceptionsStack.addAll(
                when (current) {
                    is MultiCauseException -> current.causes.mapNotNull { it }
                    else -> listOfNotNull(current.cause)
                }
            )
        }
        return matchingExceptions
    }
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

typealias BuildAction = TestProject.(buildArguments: Array<String>, buildOptions: BuildOptions) -> Unit
class ReturnFromBuildScriptAfterExecution<T>(
    val returnContainingGradleProject: TestProject,
    val serializedReturnPath: File,
    val injectionLoadProperty: String,
    val defaultEvaluationTask: String = "tasks",
    val defaultBuildAction: BuildAction = build,
) {
    /**
     * Return values to the test by serializing the return after the execution. The benefit of serializing after execution is that we can
     * query anything from relevant tasks since they have executed. However, we have to disable CC, so that the returning closure can reach
     * out for configuration entities.
     */
    fun buildAndReturn(
        vararg buildArguments: String = arrayOf(defaultEvaluationTask),
        executingProject: TestProject = returnContainingGradleProject,
        /**
         * FIXME: With enabled CC and "configuration-cache.problems=fail" if build fails due to CC serialization, Gradle will not report an
         * error in the FlowScope and it will not be caught in [catchBuildFailures]. With "configuration-cache.problems=warn" Gradle
         * always forces CC deserialization before task execution and will therefore produce a catchable build failure, but only if the
         * violating task actually executes
         */
        configurationCache: BuildOptions.ConfigurationCacheValue = BuildOptions.ConfigurationCacheValue.DISABLED,
        configurationCacheProblems: BuildOptions.ConfigurationCacheProblems = BuildOptions.ConfigurationCacheProblems.FAIL,
        deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
        buildAction: BuildAction = defaultBuildAction,
    ): T {
        executingProject.buildAction(
            arrayOf(
                *buildArguments,
                "-P${injectionLoadProperty}=true",
            ),
            executingProject.deriveBuildOptions().copy(
                configurationCache = configurationCache,
                configurationCacheProblems = configurationCacheProblems,
            )
        )
        ObjectInputStream(serializedReturnPath.inputStream()).use {
            @Suppress("UNCHECKED_CAST")
            return it.readObject() as T
        }
    }

    companion object {
        val build: BuildAction = { args, options ->
            build(*args, buildOptions = options)
        }
        val buildAndFail: BuildAction = { args, options ->
            buildAndFail(*args, buildOptions = options)
        }
    }
}

/**
 * The [returnFromProject] by default executes without CC and at build completion. If you enable CC it will execute eagerly at CC
 * serialization time.
 */
internal fun <T> TestProject.buildScriptReturn(
    returnFromProject: GradleProjectBuildScriptInjectionContext.() -> T,
) = providerBuildScriptReturn {
    project.provider {
        returnFromProject()
    }
}

/**
 * The [returnFromProject] by default executes without CC and at build completion. If you enable CC the closure will execute whenever Gradle
 * serializes the Provider value; in most cases this happens before execution, but for example if you flatMap the task output or derive
 * Provider from [providers.environmentVariable] it might execute at build completion time.
 */
internal fun <T> TestProject.providerBuildScriptReturn(
    returnFromProject: GradleProjectBuildScriptInjectionContext.() -> Provider<T>,
): ReturnFromBuildScriptAfterExecution<T> {
    return buildScriptReturnInjection(
        insertInjection = String::plus,
        injectionProvider = { serializedReturnPath ->
            OnBuildCompletionSerializingInjection(
                serializedReturnPath,
                returnFromProject,
            )
        },
        returnObjectProvider = { serializedReturnPath, injectionIdentifier ->
            ReturnFromBuildScriptAfterExecution(
                this,
                serializedReturnPath,
                injectionIdentifier,
            )
        }
    )
}

sealed class CaughtBuildFailure<ExpectedException : Throwable> : Serializable {
    data class Expected<ExpectedException : Throwable>(val matchedExceptions: Set<ExpectedException>) : CaughtBuildFailure<ExpectedException>()
    data class Unexpected<ExpectedException : Throwable>(val stackTraceDump: String) : CaughtBuildFailure<ExpectedException>()
    class UnexpectedMissingBuildFailure<ExpectedException : Throwable> : CaughtBuildFailure<ExpectedException>()

    fun unwrap(): Set<ExpectedException> {
        return when (this) {
            is Expected<ExpectedException> -> matchedExceptions
            is Unexpected<ExpectedException> -> error(stackTraceDump)
            is UnexpectedMissingBuildFailure<ExpectedException> -> error(
                """
                Build completion handler executed, but there were no failures. This likely means either:
                - Build succeeded
                - There was a CC serialization error; these are currently not caught
                """.trimIndent()
            )
        }
    }
}

/**
 * Catch all build failures of type [T] thrown at configuration or execution time. This function returns one of the following:
 * - [CaughtBuildFailure.Expected]: The caught exception of type [T] thrown some time during the build
 * - [CaughtBuildFailure.Unexpected]: The backtrace of the top level exception caught by the build when [T] wasn't found in the exception cause graph
 * - [CaughtBuildFailure.UnexpectedMissingBuildFailure]: Build was expected to fail, but no failure was reported by Gradle
 *
 * FIXME: Currently CC serialization failures are not caught
 */
internal inline fun <reified T : Exception> TestProject.catchBuildFailures(): ReturnFromBuildScriptAfterExecution<CaughtBuildFailure<T>> {
    return buildScriptReturnInjection(
        insertInjection = String::insertBlockToBuildScriptAfterPluginsAndImports,
        injectionProvider = { serializedReturnPath ->
            FindMatchingBuildFailureInjection(
                serializedReturnPath,
                T::class.java,
            )
        },
        returnObjectProvider = { serializedReturnPath, injectionIdentifier ->
            ReturnFromBuildScriptAfterExecution(
                this,
                serializedReturnPath,
                injectionIdentifier,
                defaultBuildAction = ReturnFromBuildScriptAfterExecution.buildAndFail,
            )
        }
    )
}

private fun <T> GradleProject.buildScriptReturnInjection(
    insertInjection: String.(insertion: String) -> String,
    injectionProvider: (serializedReturnPath: File) -> GradleBuildScriptInjection<Project>,
    returnObjectProvider: (serializedReturnPath: File, injectionIdentifier: String) -> ReturnFromBuildScriptAfterExecution<T>,
): ReturnFromBuildScriptAfterExecution<T> {
    enableBuildScriptInjectionsIfNecessary(
        buildGradle,
        buildGradleKts,
    )
    val injectionIdentifier = generateIdentifier()
    val serializedReturnPath = projectPath.resolve("serializedReturnConfiguration_${injectionIdentifier}").toFile()
    val injection = injectionProvider(serializedReturnPath)

    val serializedInjectionName = "serializedInjection_${injectionIdentifier}"
    val serializedInjectionPath = projectPath.resolve(serializedInjectionName)
    serializedInjectionPath.toFile().outputStream().use {
        ObjectOutputStream(it).writeObject(injection)
    }

    fun whenPropertySpecified(
        property: String,
        execute: String,
    ): String = """
    
        if (project.hasProperty("${property}")) {
            ${execute}
        }
        
    """.trimIndent()

    when {
        buildGradleKts.exists() -> buildGradleKts.modify {
            it.insertInjection(
                whenPropertySpecified(
                    injectionIdentifier,
                    injectionLoadProject(serializedInjectionPath.name)
                )
            )
        }
        buildGradle.exists() -> buildGradle.modify {
            it.insertInjection(
                whenPropertySpecified(
                    injectionIdentifier,
                    injectionLoadProjectGroovy(serializedInjectionPath.name)
                )
            )
        }
        else -> error("Can't find the build script to append the return injection")
    }

    return returnObjectProvider(serializedReturnPath, injectionIdentifier)
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
    val injection = UndispatchedInjection(
        instantiateInjectionContext,
        code,
    )

    val serializedInjectionName = "serializedConfiguration_${generateIdentifier()}"
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
