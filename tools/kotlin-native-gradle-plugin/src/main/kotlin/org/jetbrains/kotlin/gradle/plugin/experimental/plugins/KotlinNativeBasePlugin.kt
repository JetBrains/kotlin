/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.cpp.CppBinary
import org.gradle.language.plugins.NativeBasePlugin
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.SubpluginEnvironment
import org.jetbrains.kotlin.gradle.plugin.experimental.CInteropSettings
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeTestComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.*
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.CInteropTask
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.gradle.plugin.loadKotlinVersionFromResource
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompilerDownloadTask
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

class KotlinNativeBasePlugin: Plugin<ProjectInternal> {

    private val log =  Logging.getLogger(this.javaClass)

    private fun TaskContainer.createRunTestTask(
            taskName: String,
            binary: KotlinNativeTestExecutableImpl,
            compileTask: KotlinNativeCompile
    ) = create(taskName, RunTestExecutable::class.java).apply {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Executes Kotlin/Native unit tests."

            val testExecutable = compileTask.outputFile
            executable = testExecutable.absolutePath

            onlyIf { testExecutable.exists() }
            inputs.file(testExecutable)
            dependsOn(compileTask)

            // TODO: Find or implement some mechanism for test result saving.
            outputDir = project.layout.buildDirectory.dir("test-results/" + binary.names.dirName).get().asFile
        }

    private fun TaskContainer.createDummyTestTask(taskName: String, target: KonanTarget) =
            create(taskName, DefaultTask::class.java).apply {
                doLast {
                    logger.warn("""

                        The current host doesn't support running executables for target '${target.name}'.
                        Skipping tests.

                    """.trimIndent())
                }
            }

    private val KonanTarget.canRunOnHost: Boolean
        get() = this == HostManager.host

    private fun lowerCamelCase(vararg components: String) =
        if (components.isEmpty()) {
            ""
        } else {
            buildString {
                append(components[0].decapitalize())
                for (i in 1 until components.size) {
                    append(components[i].capitalize())
                }
            }
        }

    private fun Project.addTargetInfoTask() = tasks.create("targets").apply {
        group = HelpTasksPlugin.HELP_GROUP
        description = "Prints Kotlin/Native targets available for this project"
        doLast { _ ->
            components.withType(AbstractKotlinNativeComponent::class.java) { component ->
                logger.lifecycle("Component '${component.name}' targets:")
                component.konanTargets.get().forEach {
                    logger.lifecycle(it.name)
                }
                logger.lifecycle("")
            }
        }
    }

    // TODO: Rework this part: the task should be created in the binary constructor (if it is possible).
    private fun Project.addCompilationTasks() {
        val kotlinVersion = this@KotlinNativeBasePlugin.loadKotlinVersionFromResource(log)
        val assembleTask = tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        val typeToAssemble = mutableMapOf<KotlinNativeBuildType, Task>()
        val targetToAssemble = mutableMapOf<KonanTarget, Task>()

        fun TaskContainer.createSpecialAssembleTask(name: String, description: String) = create(name) {
            it.group = BasePlugin.BUILD_GROUP
            it.description = description
            assembleTask.dependsOn(it)
        }

        project.configurations.apply {
            maybeCreate(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
                isTransitive = false
            }
            maybeCreate(PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
                isTransitive = false
            }
        }

        components.withType(AbstractKotlinNativeBinary::class.java) { binary ->
            val names = binary.names
            val target = binary.konanTarget
            val buildType = binary.buildType

            val compileTask = tasks.create(
                    names.getCompileTaskName(LANGUAGE_NAME),
                    KotlinNativeCompile::class.java,
                    binary
            ).also {
                it.group = BasePlugin.BUILD_GROUP
                it.description = "Compiles Kotlin/Native source set '${binary.sourceSet.name}' into a ${binary.kind.name.toLowerCase()}"

                SubpluginEnvironment.loadSubplugins(this, kotlinVersion)
                    .addSubpluginOptions(this, it, it.compilerPluginOptions)
                it.compilerPluginClasspath = project.configurations.getByName(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME)

                // Register an API header produced for shared/static library as a task output.
                if (binary.kind == CompilerOutputKind.DYNAMIC || binary.kind == CompilerOutputKind.STATIC) {
                    val headerFileProvider = provider {
                        with(binary) {
                            val prefix = kind.prefix(konanTarget)
                            val baseName = getBaseName().get().replace('-', '_')
                            it.outputFile.parentFile.resolve("$prefix${baseName}_api.h")
                        }
                    }
                    it.outputs.file(headerFileProvider)
                }
            }
            binary.compileTask.set(compileTask)
            binary.outputs.from(compileTask.outputLocationProvider)

            when(binary) {
                is KotlinNativeExecutableImpl -> binary.runtimeFile.set(compileTask.outputLocationProvider as RegularFileProperty)
                is KotlinNativeLibraryImpl -> binary.linkFile.set(compileTask.outputLocationProvider as RegularFileProperty)
            }

            if (binary is KotlinNativeTestExecutableImpl) {
                // Generate a task for test execution.
                val taskName = binary.names.getTaskName("run")
                val testTask = if (target.canRunOnHost) {
                    tasks.createRunTestTask(taskName, binary, compileTask)
                } else {
                    // We create dummy tasks that just write an error message to avoid having an unset
                    // runTask property for targets that cannot be executed on the current host.
                    tasks.createDummyTestTask(taskName, target)
                }
                binary.runTask.set(testTask)
            } else {
                // Add dependency for assemble tasks.
                targetToAssemble.getOrPut(target) {
                    tasks.createSpecialAssembleTask(
                        "assembleAll${target.name.capitalize()}",
                        "Compiles all Kotlin/Native binaries for target '${target.name}'"
                    )
                }.dependsOn(compileTask)

                typeToAssemble.getOrPut(buildType) {
                    val buildTypeName = buildType.name
                    tasks.createSpecialAssembleTask(
                        "assembleAll${buildTypeName.capitalize()}",
                        "Compiles all ${buildTypeName} Kotlin/Native binaries for all targets"
                    )
                }.dependsOn(compileTask)

            }
        }
    }

    private fun Project.addInteropTasks() {
        val settingsToTask = mutableMapOf<CInteropSettings, CInteropTask>()
        val namesWithWarning = mutableSetOf<String>()

        fun AbstractKotlinNativeBinary.setupInteropFromComponent(component: KotlinNativeComponent) {
            component.dependencies.cinterops.all { cinterop ->
                val settings = cinterop.target(konanTarget)

                val interopTask = settingsToTask.getOrPut(settings) {
                    tasks.create(
                        lowerCamelCase("cinterop", cinterop.name, konanTarget.name),
                        CInteropTask::class.java,
                        settings
                    ).apply {
                        group = INTEROP_GROUP
                        description =
                                "Generates Kotlin/Native interop library '${cinterop.name}' for target '${konanTarget.name}'"
                    }
                }

                klibs.dependencies.add(project.dependencies.create(files(interopTask.outputFileProvider)))

                if (this is KotlinNativeLibrary) {
                    linkElements.get().outgoing.artifact(interopTask.outputFileProvider) {
                        it.classifier = "interop-${cinterop.name}"
                    }
                    // User can create an interop with the same name as the main library.
                    // In this case we get two libraries with the same name and one of them depends
                    // on the another. Such a situation is considered as a cyclic dependency by the compiler
                    // so we warn a user about it.
                    val mainLibraryName = linkFile.get().asFile.nameWithoutExtension
                    val interopLibraryName = interopTask.outputFile.nameWithoutExtension

                    if (mainLibraryName == interopLibraryName && mainLibraryName !in namesWithWarning) {
                        logger.warn(
                            """

                            Warning: you have an interop with the same name as the main library ($mainLibraryName)!
                            It may cause failures in dependent projects so consider renaming the interop.

                        """.trimIndent()
                        )
                        namesWithWarning.add(mainLibraryName)
                    }
                }
            }
        }

        components.withType(AbstractKotlinNativeBinary::class.java) { binary ->
            binary.component.let {
                binary.setupInteropFromComponent(it)
                if (it is KotlinNativeTestComponent) {
                    binary.setupInteropFromComponent(it.testedComponent)
                }
            }
        }
    }

    private fun ProjectInternal.checkGradleMetadataFeature() {
        val metadataEnabled = gradle.services
            .get(FeaturePreviews::class.java)
            .isFeatureEnabled(FeaturePreviews.Feature.GRADLE_METADATA)
        if (!metadataEnabled) {
            logger.warn(
                "The GRADLE_METADATA feature is not enabled: publication and external dependencies will not work properly."
            )
        }
    }

    private fun checkGradleVersion() =  GradleVersion.current().let { current ->
        check(current >= KonanPlugin.REQUIRED_GRADLE_VERSION) {
            "Kotlin/Native Gradle plugin is incompatible with this version of Gradle.\n" +
                    "The minimal required version is ${KonanPlugin.REQUIRED_GRADLE_VERSION}\n" +
                    "Current version is ${current}"
        }
    }

    private fun ProjectInternal.addCompilerDownloadingTask(): KonanCompilerDownloadTask {
        val result = tasks.create(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME, KonanCompilerDownloadTask::class.java)
        project.warnAboutDeprecatedProperty(KonanPlugin.ProjectProperty.KONAN_HOME)
        if (!hasProperty(KonanPlugin.ProjectProperty.KONAN_HOME)) {
            setProperty(KonanPlugin.ProjectProperty.KONAN_HOME, project.konanCompilerDownloadDir())
            setProperty(KonanPlugin.ProjectProperty.DOWNLOAD_COMPILER, true)
        }
        return result
    }

    private fun Project.setUpMatchingStrategy(): Unit = with(dependencies.attributesSchema) {
        attribute(CppBinary.DEBUGGABLE_ATTRIBUTE).apply {
            compatibilityRules.add(Compatible::class.java)
            disambiguationRules.add(DebuggableDisambiguation::class.java)
        }
        attribute(CppBinary.OPTIMIZED_ATTRIBUTE).apply {
            compatibilityRules.add(Compatible::class.java)
            disambiguationRules.add(OptimizedDisambiguation::class.java)
        }
        attribute(Usage.USAGE_ATTRIBUTE).apply {
            compatibilityRules.add(UsageCompatibility::class.java)
            disambiguationRules.add(UsageDisambiguation::class.java)
        }
    }

    override fun apply(project: ProjectInternal): Unit = with(project) {
        logger.warn("""
            |The 'kotlin-platform-native' plugin applied in the project '${project.path}' has been deprecated and will not be available since 1.3.50.
            |Consider using the 'kotlin-multiplatform' plugin.
            |
            |See details at: https://kotlinlang.org/docs/reference/native/gradle_plugin.html            
        """.trimMargin())

        // TODO: Deal with compiler downloading.
        // Apply base plugins
        project.pluginManager.apply(LifecycleBasePlugin::class.java)

        checkGradleMetadataFeature()
        checkGradleVersion()
        addCompilerDownloadingTask()

        setUpMatchingStrategy()
        addCompilationTasks()

        // The NativeBasePlugin accesses some properties which are set by the methods above
        // (e.g. linkFile for libraries). Thus we have to apply it after setting there properties.
        project.pluginManager.apply(NativeBasePlugin::class.java)

        addInteropTasks()
        addTargetInfoTask()
    }

    companion object {
        const val LANGUAGE_NAME = "KotlinNative"
        const val SOURCE_SETS_EXTENSION = "sourceSets"

        const val INTEROP_GROUP = "interop"
    }

}
