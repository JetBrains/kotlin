/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.dir
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File


internal val SetUpXCFrameworksTasksAction = KotlinProjectSetupAction {
    launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseDsl) {
        xcframeworkConfigs.forEach {
            it.configureDependentTasksAndFinalizeAllFrameworks()
        }
    }
    launchInStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
        xcframeworkConfigs.forEach {
            it.validateAllXCFrameworks()
        }
    }
}

private val Project.xcframeworkConfigs: MutableList<XCFrameworkConfig>
    get() {
        return project.extraProperties.getOrPut<MutableList<XCFrameworkConfig>>(XCFrameworkConfig.XCFRAMEWORK_CONFIGS_EXTENSION) {
            mutableListOf<XCFrameworkConfig>()
        }
    }


/**
 * XCFrameworkConfig is configuration-time entity that is responsible for:
 * 1. Registering .xcframework producing tasks (XCFrameworkTask) for each build type
 * 2. Validating that XCFrameworkTask has correct inputs
 * 3. Wiring up the dependencies of XCFrameworkTask
 *
 * .xcframework bundle can only be created with lipo'ed frameworks per platform (e.g. iphonesimulator). For example x86_64-sim and arm64-sim
 * binaries must be lipo'ed before they may be bundled in the xcframework. If needed XCFrameworkConfig creates the universal framework
 * (FatFrameworkTask) tasks and wires it into the XCFrameworkTask.
 */
class XCFrameworkConfig {
    private data class XCFrameworkDescriptor(
        val xcframeworkTask: TaskProvider<XCFrameworkTask>,
        val frameworks: MutableList<Provider<FrameworkDescriptor>> = mutableListOf(),
    )

    internal data class ConfiguredXCFrameworkTask(
        val outputDir: Provider<File>,
        val buildType: NativeBuildType,
    )

    internal data class XCFrameworkTaskConfiguration(
        val taskName: (xcframeworkConfigurationName: String) -> String,
        val group: String = BasePlugin.BUILD_GROUP,
        val description: String,
        val baseName: Provider<String>,
        val customOutputDirectory: Provider<File>? = null,
        val aggregateTask: TaskProvider<*>,
        val dependOnTasks: (ConfiguredXCFrameworkTask) -> (List<TaskProvider<*>>) = { _ -> emptyList() },
    )

    private val project: Project
    private val descriptorForBuildType: Map<NativeBuildType, XCFrameworkDescriptor>
    private val universalFrameworkTaskNamePrefix: List<String>
    private val xcframeworkIntermediatesName: String
    private val xcframeworkConfigurationName: String

    internal constructor(
        project: Project,
        buildTypes: Set<NativeBuildType>,
        universalFrameworkTaskNamePrefix: List<String> = emptyList(),
        xcframeworkConfigurationName: String,
        xcframeworkIntermediatesName: String = defaultXCFrameworkIntermediatesName,
        xcframeworkTaskConfigurationProvider: (NativeBuildType) -> XCFrameworkTaskConfiguration,
    ) {
        require(xcframeworkConfigurationName.isNotBlank())
        this.project = project
        this.universalFrameworkTaskNamePrefix = universalFrameworkTaskNamePrefix
        this.xcframeworkConfigurationName = xcframeworkConfigurationName
        this.xcframeworkIntermediatesName = xcframeworkIntermediatesName

        val descriptorForBuildType = mutableMapOf<NativeBuildType, XCFrameworkDescriptor>()
        buildTypes.forEach { buildType ->
            val xcframeworkTaskConfiguration = xcframeworkTaskConfigurationProvider(buildType)
            val xcframeworkTask = project.registerXCFrameworkTask(
                xcframeworkConfigurationName,
                xcframeworkTaskConfiguration,
                buildType
            )
            xcframeworkTaskConfiguration.aggregateTask.dependsOn(xcframeworkTask)
            descriptorForBuildType[buildType] = XCFrameworkDescriptor(xcframeworkTask)

            xcframeworkTaskConfiguration.dependOnTasks(
                ConfiguredXCFrameworkTask(
                    xcframeworkTask.map { it.outputDir },
                    buildType,
                )
            ).forEach {
                xcframeworkTask.dependsOn(it)
            }
        }

        this.descriptorForBuildType = descriptorForBuildType
        project.xcframeworkConfigs.add(this)
    }

    constructor(project: Project, xcFrameworkName: String, buildTypes: Set<NativeBuildType>) : this(
        project = project,
        buildTypes = buildTypes,
        xcframeworkConfigurationName = xcFrameworkName,
        xcframeworkTaskConfigurationProvider = { buildType ->
            XCFrameworkTaskConfiguration(
                taskName = { xcFrameworkName -> defaultXCFrameworkTaskName(buildType, xcFrameworkName) },
                baseName = project.provider { xcFrameworkName },
                description = defaultXCFrameworkTaskDescription(buildType),
                aggregateTask = project.parentAssembleXCFrameworkTask(xcFrameworkName),
            )
        },
    )
    constructor(project: Project) : this(project, project.name)
    constructor(project: Project, xcFrameworkName: String) : this(project, xcFrameworkName, NativeBuildType.values().toSet())

    /**
     * Adds the specified frameworks in this XCFramework.
     */
    fun add(framework: Framework) = add(
        frameworkDescriptor = framework.linkTaskProvider.map { FrameworkDescriptor(framework) },
        buildType = framework.buildType
    )

    internal fun add(
        frameworkDescriptor: Provider<FrameworkDescriptor>,
        buildType: NativeBuildType,
    ) {
        if (isFrameworkListFinalized) {
            project.reportDiagnostic(
                KotlinToolingDiagnostics.AddingFrameworkToXCFrameworkAfterDSLFinalized(
                    xcFramework = xcframeworkConfigurationName,
                    buildType = buildType,
                    framework = frameworkDescriptor.get().name,
                )
            )
        }
        val descriptor = descriptorForBuildType[buildType]
        if (descriptor == null) {
            project.reportDiagnostic(
                KotlinToolingDiagnostics.MissingBuildTypeInXCFrameworkConfiguration(
                    xcFrameworkConfigation = xcframeworkConfigurationName,
                    buildType = buildType,
                )
            )
            return
        }
        descriptor.frameworks.add(frameworkDescriptor)
    }

    private var isFrameworkListFinalized = false
    internal fun configureDependentTasksAndFinalizeAllFrameworks() {
        descriptorForBuildType.forEach {
            configureDependentTasksAndFinalizeFrameworks(
                it.value,
                it.key
            )
        }
    }

    private fun configureDependentTasksAndFinalizeFrameworks(
        xcFrameworkDescriptor: XCFrameworkDescriptor,
        buildType: NativeBuildType,
    ) {
        isFrameworkListFinalized = true
        xcFrameworkDescriptor.frameworks
            .groupBy { descriptorProvider ->
                val descriptor = descriptorProvider.get()
                universalGroupFromTarget[descriptor.target] ?: error("Target ${descriptor.target.name} is missing a universal framework group")
            }
            .forEach {
                val universalGroupName = it.key
                val frameworkDescriptors = it.value

                if (frameworkDescriptors.size > 1) {
                    val universalFrameworkTaskProvider = project.registerUniversalFrameworkTask(
                        buildType = buildType,
                        universalGroupName = universalGroupName,
                        universalFrameworkName = frameworkDescriptors[0].get().name,
                        frameworkDescriptors = frameworkDescriptors,
                    )
                    xcFrameworkDescriptor.xcframeworkTask.configure {
                        it.xcframeworkSlices.add(
                            universalFrameworkTaskProvider.map { universalFrameworkTask ->
                                XCFrameworkTask.XCFrameworkSlice(
                                    universalFrameworkTask.fatFramework,
                                    frameworkDescriptors[0].get().isStatic,
                                )
                            }
                        )
                    }
                } else {
                    val framework = frameworkDescriptors.single()
                    xcFrameworkDescriptor.xcframeworkTask.configure {
                        it.xcframeworkSlices.add(
                            framework.map { descriptor ->
                                XCFrameworkTask.XCFrameworkSlice(
                                    descriptor.file,
                                    descriptor.isStatic,
                                )
                            }
                        )
                    }
                }
            }
    }

    internal fun validateAllXCFrameworks() {
        descriptorForBuildType.forEach {
            validate(it.value)
        }
    }

    private fun validate(xcframeworkDescriptor: XCFrameworkDescriptor) {
        xcframeworkDescriptor.xcframeworkTask.configure {
            with(it) {
                val xcframeworkBaseName = baseName.get()
                val frameworkDescriptors = xcframeworkDescriptor.frameworks.map { it.get() }
                if (frameworkDescriptors.isEmpty()) {
                    project.reportDiagnostic(
                        KotlinToolingDiagnostics.XCFrameworkHasNoFrameworks(
                            xcFramework = xcframeworkBaseName,
                            buildType = buildType,
                        )
                    )
                    return@with
                }

                val expectedFrameworkName = frameworkDescriptors[0].name
                if (frameworkDescriptors.any { it.name != expectedFrameworkName }) {
                    project.reportDiagnostic(
                        KotlinToolingDiagnostics.FrameworksInXCFrameworkHaveDifferentNames(
                            xcFramework = xcframeworkBaseName,
                            buildType = buildType,
                            frameworkPaths = frameworkDescriptors.map { it.file.path },
                        )
                    )
                    configurationError.set(
                        KotlinToolingDiagnostics.FrameworksInXCFrameworkHaveDifferentNames.errorText(
                            xcFramework = xcframeworkBaseName,
                            buildType = buildType,
                            frameworkPaths = frameworkDescriptors.map { it.file.path },
                        )
                    )
                }
                val xcframeworkName = xcFrameworkName.get()
                if (expectedFrameworkName != xcframeworkName) {
                    project.reportDiagnostic(
                        KotlinToolingDiagnostics.XCFrameworkNameIsDifferentFromInnerFrameworksName(
                            xcFramework = xcframeworkBaseName,
                            innerFrameworks = expectedFrameworkName,
                        )
                    )
                }
            }
        }
    }

    private fun Project.registerXCFrameworkTask(
        xcFrameworkTaskName: String,
        configuration: XCFrameworkTaskConfiguration,
        buildType: NativeBuildType,
    ): TaskProvider<XCFrameworkTask> {
        val taskName = configuration.taskName(xcFrameworkTaskName)
        return registerTask(taskName) { task ->
            task.buildType = buildType
            task.baseName = configuration.baseName
            task.group = configuration.group
            task.description = configuration.description
            configuration.customOutputDirectory?.let { task.outputDirProperty.set(it) }
        }
    }

    private fun Project.registerUniversalFrameworkTask(
        buildType: NativeBuildType,
        universalGroupName: String,
        universalFrameworkName: String,
        frameworkDescriptors: List<Provider<FrameworkDescriptor>>,
    ): TaskProvider<FatFrameworkTask> {
        return registerTask(
            universalFrameworkTaskName(
                universalFrameworkTaskNamePrefix,
                buildType,
                universalGroupName,
                xcframeworkConfigurationName,
            )
        ) { universalFrameworkTask ->
            universalFrameworkTask.destinationDirProperty.set(
                universalFrameworkDir(
                    project,
                    xcframeworkIntermediatesName,
                    xcframeworkConfigurationName,
                    buildType,
                    universalGroupName
                )
            )
            universalFrameworkTask.baseName = universalFrameworkName
            universalFrameworkTask.fromFrameworkDescriptorProviders(frameworkDescriptors)
        }
    }

    internal companion object {
        const val XCFRAMEWORK_CONFIGS_EXTENSION = "${PropertiesProvider.KOTLIN_INTERNAL_NAMESPACE}.xcframeworkConfigs"

        internal fun defaultXCFrameworkTaskName(
            buildType: NativeBuildType,
            xcFrameworkName: String,
        ) = lowerCamelCaseName(
            "assemble",
            xcFrameworkName,
            buildType.getName(),
            "XCFramework"
        )

        internal fun defaultXCFrameworkTaskDescription(
            buildType: NativeBuildType
        ): String = "Assemble .xcframework bundle for build type ${buildType.name}"

        private fun universalFrameworkTaskName(
            prefix: List<String>,
            buildType: NativeBuildType,
            universalGroupName: String,
            xcFrameworkName: String,
        ) = lowerCamelCaseName(
            *prefix.toTypedArray(),
            "assemble",
            buildType.getName(),
            universalGroupName,
            "UniversalFrameworkFor",
            xcFrameworkName,
            "XCFramework"
        )

        private val defaultXCFrameworkIntermediatesName = "XCFrameworkTemp"
        private fun universalFrameworkDir(
            project: Project,
            xcFrameworkIntermediatesName: String,
            xcFrameworkName: String,
            buildType: NativeBuildType,
            universalGroupName: String,
        ): Provider<Directory> = project.layout.buildDirectory
            .dir(xcFrameworkIntermediatesName)
            .dir(xcFrameworkName.asValidFrameworkName())
            .dir("universalFramework")
            .dir(buildType.getName())
            .dir(universalGroupName)

        private val macosGroup = "macos"
        private val iOSGroup = "ios"
        private val iOSSimulatorGroup = "iosSimulator"
        private val watchOSGroup = "watchos"
        private val watchOSSimulatorGroup = "watchosSimulator"
        private val tvOSGroup = "tvos"
        private val tvOSSimulatorGroup = "tvosSimulator"
        private val universalGroupFromTarget = mapOf(
            KonanTarget.MACOS_X64               to macosGroup,
            KonanTarget.MACOS_ARM64             to macosGroup,
            KonanTarget.IOS_ARM64               to iOSGroup,
            KonanTarget.IOS_X64                 to iOSSimulatorGroup,
            KonanTarget.IOS_SIMULATOR_ARM64     to iOSSimulatorGroup,
            KonanTarget.WATCHOS_ARM32           to watchOSGroup,
            KonanTarget.WATCHOS_ARM64           to watchOSGroup,
            KonanTarget.WATCHOS_DEVICE_ARM64    to watchOSGroup,
            KonanTarget.WATCHOS_X64             to watchOSSimulatorGroup,
            KonanTarget.WATCHOS_SIMULATOR_ARM64 to watchOSSimulatorGroup,
            KonanTarget.TVOS_ARM64              to tvOSGroup,
            KonanTarget.TVOS_X64                to tvOSSimulatorGroup,
            KonanTarget.TVOS_SIMULATOR_ARM64    to tvOSSimulatorGroup,
        )
    }
}

fun Project.XCFramework(xcFrameworkName: String = name) = XCFrameworkConfig(this, xcFrameworkName)

private fun Project.eraseIfDefault(xcFrameworkName: String) =
    if (name == xcFrameworkName) "" else xcFrameworkName

private fun Project.parentAssembleXCFrameworkTask(xcFrameworkName: String): TaskProvider<Task> =
    locateOrRegisterTask(lowerCamelCaseName("assemble", eraseIfDefault(xcFrameworkName), "XCFramework")) {
        it.group = "build"
        it.description = "Assemble all types of registered '$xcFrameworkName' XCFramework"
    }

