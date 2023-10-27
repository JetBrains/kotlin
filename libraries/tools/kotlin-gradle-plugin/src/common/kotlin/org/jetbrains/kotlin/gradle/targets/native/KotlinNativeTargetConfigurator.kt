/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Exec
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeVersionTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedAndSignAppleFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.version
import org.jetbrains.kotlin.gradle.artifacts.createKlibArtifact
import org.jetbrains.kotlin.gradle.artifacts.klibOutputDirectory
import org.jetbrains.kotlin.gradle.targets.native.*
import org.jetbrains.kotlin.gradle.targets.native.internal.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.whenEvaluated
import org.jetbrains.kotlin.gradle.utils.XcodeUtils
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.konan.target.HostManager

open class KotlinNativeTargetConfigurator<T : KotlinNativeTarget> : AbstractKotlinTargetConfigurator<T>(
    createTestCompilation = true
) {


    // FIXME support creating interop tasks for PM20
    private fun Project.createCInteropTasks(
        compilation: KotlinNativeCompilation,
        cinterops: NamedDomainObjectCollection<DefaultCInteropSettings>,
    ) {
        val compilationInfo = KotlinCompilationInfo(compilation)
        cinterops.all { interop ->

            val params = CInteropProcess.Params(
                settings = interop,
                targetName = compilation.target.name,
                compilationName = compilation.name,
                konanTarget = compilation.konanTarget,
                baseKlibName = run {
                    val compilationPrefix = if (compilation.isMain()) project.name else compilation.name
                    "$compilationPrefix-cinterop-${interop.name}"
                },
                services = objects.newInstance()
            )

            val interopTask = registerTask<CInteropProcess>(interop.interopProcessingTaskName, listOf(params)) {
                it.destinationDir = klibOutputDirectory(compilationInfo).dir("cinterop").map { it.asFile }
                it.group = INTEROP_GROUP
                it.description = "Generates Kotlin/Native interop library '${interop.name}' " +
                        "for compilation '${compilation.compilationName}'" +
                        "of target '${it.konanTarget.name}'."
                it.enabled = compilation.konanTarget.enabledOnCurrentHost
            }


            project.launch {
                project.commonizeCInteropTask()?.configure { commonizeCInteropTask ->
                    commonizeCInteropTask.from(interopTask)
                }
                project.copyCommonizeCInteropForIdeTask()
            }

            val interopOutput = project.files(interopTask.map { it.outputFileProvider })
            with(compilation) {
                compileDependencyFiles += interopOutput
                if (isMain()) {
                    // Add interop library to special CInteropApiElements configuration
                    createCInteropApiElementsKlibArtifact(compilation.target, interop, interopTask)

                    // Add the interop library in publication.
                    createKlibArtifact(
                        compilation,
                        artifactFile = interopTask.map { it.outputFile },
                        classifier = "cinterop-${interop.name}",
                        producingTask = interopTask,
                    )

                    // We cannot add the interop library in an compilation output because in this case
                    // IDE doesn't see this library in module dependencies. So we have to manually add
                    // main interop libraries in dependencies of the default test compilation.
                    target.compilations.findByName(TEST_COMPILATION_NAME)?.let { testCompilation ->
                        testCompilation.compileDependencyFiles += interopOutput
                        testCompilation.cinterops.all {
                            it.dependencyFiles += interopOutput
                        }
                    }
                }
            }
        }
    }
    // endregion.

    // region Configuration.
    override fun configurePlatformSpecificModel(target: T) {
        configureFrameworkExport(target)
        configureCInterops(target)

        if (target.konanTarget.family.isAppleFamily) {
            registerEmbedAndSignAppleFrameworkTasks(target)
        }

        if (PropertiesProvider(target.project).ignoreIncorrectNativeDependencies != true) {
            warnAboutIncorrectDependencies(target)
        }
    }

    protected fun configureCInterops(target: KotlinNativeTarget): Unit = with(target.project) {
        locateOrCreateCInteropApiElementsConfiguration(target)
        target.compilations.all { compilation ->
            createCInteropTasks(compilation, compilation.cinterops)
            compilation.cinterops.all { cinterop ->
                cinterop.dependencyFiles += locateOrCreateCInteropDependencyConfiguration(compilation)
            }
        }
    }



    fun configureFrameworkExport(target: KotlinNativeTarget) {
        val project = target.project

        target.compilations.all {
            // Allow resolving api configurations directly to be able to check that
            // all exported dependency are also added in the corresponding api configurations.
            // The check is performed during a link task execution.
            project.configurations.maybeCreate(it.apiConfigurationName).apply {
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
        }

        target.binaries.withType(AbstractNativeLibrary::class.java).all { framework ->
            project.configurations.maybeCreate(framework.exportConfigurationName).apply {
                isVisible = false
                isTransitive = false
                isCanBeConsumed = false
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                description = "Dependenceis to be exported in framework ${framework.name} for target ${target.targetName}"
            }
        }
    }

    private fun registerEmbedAndSignAppleFrameworkTasks(target: KotlinNativeTarget) {
        val project = target.project
        target.binaries.withType(Framework::class.java).all { framework ->
            project.registerEmbedAndSignAppleFrameworkTask(framework)
        }
    }

    private fun warnAboutIncorrectDependencies(target: KotlinNativeTarget) = target.project.launchInStage(ReadyForExecution) {

        val compileOnlyDependencies = target.compilations.mapNotNull {
            val dependencies = project.configurations.getByName(it.compileOnlyConfigurationName).allDependencies
            if (dependencies.isNotEmpty()) {
                it to dependencies
            } else null
        }

        fun Dependency.stringCoordinates(): String = buildString {
            group?.let { append(it).append(':') }
            append(name)
            version?.let { append(':').append(it) }
        }

        if (compileOnlyDependencies.isNotEmpty()) {
            with(target.project.logger) {
                warn("A compileOnly dependency is used in the Kotlin/Native target '${target.name}':")
                compileOnlyDependencies.forEach {
                    warn(
                        """
                        Compilation: ${it.first.name}

                        Dependencies:
                        ${it.second.joinToString(separator = "\n") { it.stringCoordinates() }}

                        """.trimIndent()
                    )
                }
                warn(
                    """
                    Such dependencies are not applicable for Kotlin/Native, consider changing the dependency type to 'implementation' or 'api'.
                    To disable this warning, set the $KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES=true project property
                    """.trimIndent()
                )
            }
        }
    }
    // endregion.

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
        const val FRAMEWORK = "org.jetbrains.kotlin.framework"
    }

    companion object {
        const val INTEROP_GROUP = "interop"
        const val RUN_GROUP = "run"


    }
}

