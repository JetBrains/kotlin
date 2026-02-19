/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv

import org.jetbrains.kotlin.build.bcv.internal.BcvProperties
import org.jetbrains.kotlin.build.bcv.internal.declarable
import org.jetbrains.kotlin.build.bcv.internal.resolvable
import org.jetbrains.kotlin.build.bcv.tasks.BcvApiCheckTask
import org.jetbrains.kotlin.build.bcv.tasks.BcvApiDumpTask
import org.jetbrains.kotlin.build.bcv.tasks.BcvApiGenerateTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import javax.inject.Inject

abstract class BcvCompatPlugin
@Inject
constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : Plugin<Project> {

    override fun apply(project: Project) {
        // apply the base plugin so the 'check' task is available
        project.pluginManager.apply(LifecycleBasePlugin::class)

        val extension = createExtension(project)

        val bcvGenerateClasspath = createBcvRuntimeClasspath(project, extension)

        project.tasks.withType<BcvApiGenerateTask>().configureEach {
            group = TASK_GROUP
            runtimeClasspath.from(bcvGenerateClasspath)
            targets.addAllLater(providers.provider { extension.targets })
            onlyIf("Must have at least one target") { targets.isNotEmpty() }
            outputApiBuildDir.convention(layout.buildDirectory.dir("bcv-api"))
            projectName.convention(extension.projectName)
        }

        project.tasks.withType<BcvApiCheckTask>().configureEach {
            group = TASK_GROUP
            outputs.dir(temporaryDir) // dummy output, so up-to-date checks work
            expectedProjectName.convention(extension.projectName)
            expectedApiDirPath.convention(extension.outputApiDir.map { it.asFile.canonicalFile.absolutePath })
        }

        project.tasks.withType<BcvApiDumpTask>().configureEach {
            group = TASK_GROUP
            apiDirectory.convention(extension.outputApiDir)

            // MUST run the API checks _before_ the API dumps, otherwise the dumps may be updated
            // before the check task, meaning the dumps are always up-to-date and apiCheck will never fail.
            mustRunAfter(project.tasks.withType<BcvApiCheckTask>())
        }

        val apiGenerateTask = project.tasks.register<BcvApiGenerateTask>(API_GENERATE_TASK_NAME)

        project.tasks.register<BcvApiDumpTask>(API_DUMP_TASK_NAME) {
            apiDumpFiles.from(apiGenerateTask.map { it.outputApiBuildDir })
        }

        val apiCheckTask = project.tasks.register<BcvApiCheckTask>(API_CHECK_TASK_NAME) {
            apiBuildDir.convention(apiGenerateTask.flatMap { it.outputApiBuildDir })
        }

        project.tasks.named(CHECK_TASK_NAME).configure {
            dependsOn(apiCheckTask)
        }
    }

    private fun createExtension(project: Project): BcvCompatProjectExtension {
        val extension = project.extensions.create(EXTENSION_NAME, BcvCompatProjectExtension::class).apply {
            enabled.convention(true)
            outputApiDir.convention(layout.projectDirectory.dir(API_DIR))
            projectName.convention(providers.provider { project.name })
            kotlinxBinaryCompatibilityValidatorVersion.convention(BcvProperties.KOTLINX_BCV_VERSION)
        }

        extension.targets.configureEach {
            enabled.convention(extension.enabled)

            publicMarkers.convention(extension.publicMarkers)
            publicPackages.convention(extension.publicPackages)
            publicClasses.convention(extension.publicClasses)

            ignoredClasses.convention(extension.ignoredClasses)
            ignoredMarkers.convention(
                extension.ignoredMarkers.orElse(
                    @Suppress("DEPRECATION")
                    extension.nonPublicMarkers
                )
            )
            ignoredPackages.convention(extension.ignoredPackages)
        }

        return extension
    }

    private fun createBcvRuntimeClasspath(
        project: Project,
        extension: BcvCompatProjectExtension,
    ): NamedDomainObjectProvider<Configuration> {

        val bcvGenerateClasspath =
            project.configurations.register(RUNTIME_CLASSPATH_CONFIGURATION_NAME) {
                description = "Runtime classpath for running binary-compatibility-validator."
                declarable()
                defaultDependencies {
                    addLater(
                        extension.kotlinxBinaryCompatibilityValidatorVersion.map { version ->
                            project.dependencies.create(
                                "org.jetbrains.kotlinx:binary-compatibility-validator:$version"
                            )
                        }
                    )
                }
            }

        return project.configurations.register(RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME) {
            description = "Resolve the runtime classpath for running binary-compatibility-validator."
            resolvable()
            extendsFrom(bcvGenerateClasspath.get())
        }
    }

    companion object {
        const val API_DIR = "api"
        const val EXTENSION_NAME = "binaryCompatibilityValidator"
        const val RUNTIME_CLASSPATH_CONFIGURATION_NAME = "bcvCompatRuntime"
        const val RUNTIME_CLASSPATH_RESOLVER_CONFIGURATION_NAME = "bcvCompatRuntimeResolver"

        const val TASK_GROUP = "bcv"
        const val API_CHECK_TASK_NAME = "apiCheck"
        const val API_DUMP_TASK_NAME = "apiDump"
        const val API_GENERATE_TASK_NAME = "apiGenerate"
    }
}
