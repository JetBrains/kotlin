/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.analysis.utils.collections.buildSmartList
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportDSLConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportedModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.BuildSPMSwiftExportPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.MergeStaticLibrariesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTask
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.unitTests.utils.applyEmbedAndSignEnvironment
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwiftExportUnitTests {
    @BeforeTest
    fun runOnMacOSOnly() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
    }

    @Test
    fun `test swift export compilation`() {
        val project = swiftExportProject()
        project.evaluate()

        val compilations = project.multiplatformExtension.iosSimulatorArm64().compilations
        val mainCompilation = compilations.main
        val swiftExportCompilation = compilations.swiftExport

        val mainCompileTask = mainCompilation.compileTaskProvider.get()
        val swiftExportCompileTask = swiftExportCompilation.compileTaskProvider.get()

        // Main compilation exist
        assertNotNull(mainCompilation)

        // swiftExportMain compilation exist
        assertNotNull(swiftExportCompilation)

        val swiftExportMainAssociation = swiftExportCompilation.associatedCompilations.single()

        // swiftExportMain associated with main
        assertEquals(swiftExportMainAssociation, mainCompilation)

        val mainKlib = mainCompileTask.outputFile.get()
        val swiftExportFriendPaths = swiftExportCompileTask.compilation.friendPaths.files

        // Swift Export compilation have dependency on main compilation
        assert(swiftExportFriendPaths.contains(mainKlib)) { "Swift Export compile task doesn't contain dependency on main klib" }

        val swiftExportLibraries = swiftExportCompileTask.libraries.files

        // Swift Export libraries contains main klib
        assert(swiftExportLibraries.contains(mainKlib))
    }

    @Test
    fun `test swift export embed and sign`() {
        val project = swiftExportProject()
        project.evaluate()

        val swiftExportTask = project.tasks.getByName("iosSimulatorArm64DebugSwiftExport")
        val generateSPMPackageTask = project.tasks.getByName("iosSimulatorArm64DebugGenerateSPMPackage")
        val buildSPMPackageTask = project.tasks.getByName("iosSimulatorArm64DebugBuildSPMPackage")
        val linkSwiftExportBinaryTask = project.tasks.getByName("linkSwiftExportBinaryDebugStaticIosSimulatorArm64")
        val mergeLibrariesTask = project.tasks.getByName("mergeIosSimulatorDebugSwiftExportLibraries")
        val copySwiftExportTask = project.tasks.getByName("copyDebugSPMIntermediates")
        val embedSwiftExportTask = project.tasks.getByName("embedSwiftExportForXcode")

        // Check embedAndSign task dependencies
        val embedAndSignTaskDependencies = embedSwiftExportTask.taskDependencies.getDependencies(null)
        assert(embedAndSignTaskDependencies.contains(copySwiftExportTask))

        // Check copySwiftExport task dependencies
        val copySwiftExportTaskDependencies = copySwiftExportTask.taskDependencies.getDependencies(null)
        assert(copySwiftExportTaskDependencies.contains(generateSPMPackageTask))
        assert(copySwiftExportTaskDependencies.contains(buildSPMPackageTask))
        assert(copySwiftExportTaskDependencies.contains(mergeLibrariesTask))

        // Check mergeLibraries task dependencies
        val mergeLibrariesTaskDependencies = mergeLibrariesTask.taskDependencies.getDependencies(null)
        assert(mergeLibrariesTaskDependencies.contains(linkSwiftExportBinaryTask))
        assert(mergeLibrariesTaskDependencies.contains(buildSPMPackageTask))

        // Check buildSPMPackage task dependencies
        val buildSPMPackageTaskDependencies = buildSPMPackageTask.taskDependencies.getDependencies(null)
        assert(buildSPMPackageTaskDependencies.contains(generateSPMPackageTask))

        // Check generateSPMPackage task dependencies
        val generateSPMPackageTaskDependencies = generateSPMPackageTask.taskDependencies.getDependencies(null)
        assert(generateSPMPackageTaskDependencies.contains(swiftExportTask))
    }

    @Test
    fun `test swift export missing arch`() {
        val project = swiftExportProject(archs = "arm64", multiplatform = {
            listOf(iosSimulatorArm64(), iosX64(), iosArm64())
        })

        project.evaluate()

        val embedSwiftExportTask = project.tasks.getByName("embedSwiftExportForXcode")

        val buildType = embedSwiftExportTask.inputs.properties["type"] as NativeBuildType

        val arm64SimLib = project.multiplatformExtension.iosSimulatorArm64().binaries.findStaticLib("SwiftExportBinary", buildType)
        val arm64Lib = project.multiplatformExtension.iosArm64().binaries.findStaticLib("SwiftExportBinary", buildType)
        val x64Lib = project.multiplatformExtension.iosX64().binaries.findStaticLib("SwiftExportBinary", buildType)

        assertNotNull(arm64SimLib)
        assertNull(arm64Lib)
        assertNull(x64Lib)

        val mergeTask = project.tasks.withType(MergeStaticLibrariesTask::class.java).single()
        val linkTask = mergeTask.taskDependencies.getDependencies(null).filterIsInstance<KotlinNativeLink>().single()

        assertEquals(linkTask.binary.buildType, buildType)
        assertEquals(linkTask.konanTarget, arm64SimLib.konanTarget)
    }

    @Test
    fun `test swift export embed and sign inputs`() {
        val project = swiftExportProject(archs = "arm64 x86_64", multiplatform = {
            listOf(iosSimulatorArm64(), iosX64())
        })

        project.evaluate()

        val embedSwiftExportTask = project.tasks.getByName("embedSwiftExportForXcode")

        @Suppress("UNCHECKED_CAST")
        val targets = embedSwiftExportTask.inputs.properties["targets"] as List<KonanTarget>
        val buildType = embedSwiftExportTask.inputs.properties["type"] as NativeBuildType

        assert(targets.contains(KonanTarget.IOS_SIMULATOR_ARM64)) { "Targets doesn't contain ios_simulator_arm64" }
        assert(targets.contains(KonanTarget.IOS_X64)) { "Targets doesn't contain ios_x64" }

        assertEquals(buildType, NativeBuildType.DEBUG, "Not a DEBUG configuration")

        val appleTarget = targets.map { it.appleTarget }.distinct().single()

        assertEquals(appleTarget, AppleTarget.IPHONE_SIMULATOR, "Target is not iOS Simulator")

        val swiftExportTasks = project.tasks.withType(SwiftExportTask::class.java)
        val buildSPMTasks = project.tasks.withType(BuildSPMSwiftExportPackage::class.java)
        val buildTypeName = buildType.getName()
        val iosArm64Prefix = "iosSimulatorArm64"
        val iosX64Prefix = "iosX64"

        fun swiftExportExpectedTaskName(prefix: String): String = lowerCamelCaseName(
            prefix,
            buildTypeName,
            "swiftExport"
        )

        // Swift Export should be registered
        assertEquals(
            swiftExportTasks.map { it.name }.first { it.startsWith(iosArm64Prefix) },
            swiftExportExpectedTaskName(iosArm64Prefix),
            "Swift Export task name doesn't match expected prefix $iosArm64Prefix"
        )

        assertEquals(
            swiftExportTasks.map { it.name }.first { it.startsWith(iosX64Prefix) },
            swiftExportExpectedTaskName(iosX64Prefix),
            "Swift Export task name doesn't match expected prefix $iosX64Prefix"
        )

        fun buildSPMTaskName(prefix: String): String = lowerCamelCaseName(
            prefix,
            buildTypeName,
            "BuildSPMPackage"
        )

        assertEquals(
            buildSPMTasks.map { it.name }.first { it.startsWith(iosArm64Prefix) },
            buildSPMTaskName(iosArm64Prefix),
            "Build SPM task name doesn't match expected prefix $iosArm64Prefix"
        )

        assertEquals(
            buildSPMTasks.map { it.name }.first { it.startsWith(iosX64Prefix) },
            buildSPMTaskName(iosX64Prefix),
            "Build SPM task name doesn't match expected prefix $iosX64Prefix"
        )
    }

    @Test
    fun `test swift export multiple modules tasks graph`() {
        val projects = multiModuleSwiftExportProject(subprojects = listOf("subproject", "anotherproject", "miniproject"))
        projects.forEach { it.evaluate() }
        val project = projects[0]
        val subProject1 = projects[1]
        val subProject2 = projects[2]
        val subProject3 = projects[3]

        val projectCompileTask = project.tasks.getByName("compileKotlinIosSimulatorArm64")
        val subProject1CompileTask = subProject1.tasks.getByName("compileKotlinIosSimulatorArm64")
        val subProject2CompileTask = subProject2.tasks.getByName("compileKotlinIosSimulatorArm64")
        val subProject3CompileTask = subProject3.tasks.getByName("compileKotlinIosSimulatorArm64")

        val projectCompileTaskDependencies = projectCompileTask.taskDependencies.getDependencies(null)
        assert(projectCompileTaskDependencies.contains(subProject1CompileTask))
        assert(projectCompileTaskDependencies.contains(subProject2CompileTask))
        assert(projectCompileTaskDependencies.contains(subProject3CompileTask))
    }

    @Test
    fun `test swift export exported modules`() {
        val projects = multiModuleSwiftExportProject {
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")

            binaries {
                linkTaskProvider.configure {
                    freeCompilerArgs += "-opt-in=some.value"
                }
            }
        }

        projects.forEach { it.evaluate() }
        val project = projects.first()

        val embedSwiftExportTask = project.tasks.getByName("embedSwiftExportForXcode")
        val buildType = embedSwiftExportTask.inputs.properties["type"] as NativeBuildType
        val arm64SimLib = project.multiplatformExtension.iosSimulatorArm64().binaries.findStaticLib(
            SwiftExportDSLConstants.SWIFT_EXPORT_LIBRARY_PREFIX,
            buildType
        )

        assertNotNull(arm64SimLib)
        assertEquals(arm64SimLib.freeCompilerArgs.single(), "-opt-in=some.value")

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.get()

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("Subproject", "subproject.klib"))
            add(SwiftExportModuleForAssertion("KotlinxCoroutinesCore", "kotlinx-coroutines-core.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test non-exported compilation dependencies are not exported`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                    implementation("com.arkivanov.decompose:decompose:3.1.0")
                }
            },
            swiftExport = {
                export("com.arkivanov.decompose:decompose:3.1.0")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.get()

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("Decompose", "decompose.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXDatetime = actualModules.singleOrNull { it.moduleName == "KotlinxDatetime" }
        assertNull(kotlinXDatetime, "Transitive dependency kotlinx-datetime should not be exported")
    }

    @Test
    fun `test transitive dependencies of exported dependencies are not exported`() {
        val project = swiftExportProject(
            swiftExport = {
                export("com.arkivanov.decompose:decompose:3.1.0")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.get()

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("Decompose", "decompose.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.singleOrNull { it.moduleName == "KotlinxCoroutinesCore" }
        assertNull(kotlinXCoroutines, "Transitive dependency kotlinx-coroutines-core should not be exported")
    }

    @Test
    fun `test compile dependency version is lower than exported dependency`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.get()

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("KotlinxCoroutinesCore", "kotlinx-coroutines-core.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.single()
        assertContains("/1.9.0-RC/", kotlinXCoroutines.artifact.path)
        assertNotContains("/1.8.0/", kotlinXCoroutines.artifact.path)
    }

    @Test
    fun `test compile dependency version is greater than exported dependency`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("KotlinxCoroutinesCore", "kotlinx-coroutines-core.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.single()
        assertContains("/1.9.0-RC/", kotlinXCoroutines.artifact.path)
        assertNotContains("/1.8.0/", kotlinXCoroutines.artifact.path)
    }

    @Test
    fun `test exported dependency version undefined`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("KotlinxCoroutinesCore", "kotlinx-coroutines-core.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.single()
        assertContains("/1.9.0-RC/", kotlinXCoroutines.artifact.path)
    }

    @Test
    fun `test dependency export custom parameters`() {
        val project = swiftExportProject(
            swiftExport = {
                export("com.arkivanov.decompose:decompose:3.1.0") {
                    moduleName.set("CustomDecompose")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = buildSmartList<SwiftExportModuleForAssertion> {
            add(SwiftExportModuleForAssertion("CustomDecompose", "decompose.klib"))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test swift export experimental feature message`() {
        val project = swiftExportProject()
        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.ExperimentalFeatureWarning)
    }
}

private fun multiModuleSwiftExportProject(
    mainProjectName: String = "shared",
    subprojects: List<String> = listOf("subproject"),
    code: SwiftExportExtension.() -> Unit = {},
): List<ProjectInternal> {
    val multiplatform: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64() }
    val project = buildProject(
        projectBuilder = {
            withName(mainProjectName)
        },
        configureProject = {
            configureRepositoriesForTests()
        }
    )
    val projectDependencies = subprojects.map { project.subProject(it, multiplatform) }
    project.setupForSwiftExport(multiplatform = multiplatform) {
        projectDependencies.forEach { export(it) }
        code()
    }

    return listOf(project) + projectDependencies
}

private fun swiftExportProject(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftExport: SwiftExportExtension.() -> Unit = {},
): ProjectInternal = buildProjectWithMPP(
    preApplyCode = {
        applyEmbedAndSignEnvironment(
            configuration = configuration,
            sdk = sdk,
            archs = archs,
        )
        configureRepositoriesForTests()
        enableSwiftExport()
    },
    code = {
        kotlin {
            multiplatform()
            swiftExport {
                swiftExport()
            }
        }
    }
)

private fun ProjectInternal.setupForSwiftExport(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftExport: SwiftExportExtension.() -> Unit = {},
) {
    applyEmbedAndSignEnvironment(
        configuration = configuration,
        sdk = sdk,
        archs = archs,
    )
    enableSwiftExport()
    applyMultiplatformPlugin()
    kotlin {
        multiplatform()
        swiftExport {
            swiftExport()
        }
    }
}

private fun ProjectInternal.subProject(
    name: String,
    multiplatform: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64() },
): ProjectInternal = buildProjectWithMPP(
    projectBuilder = {
        withParent(this@subProject)
        withName(name)
    },
    code = {
        kotlin {
            multiplatform()
        }
    }
)

private fun List<SwiftExportedModule>.toModulesForAssertion() = map { SwiftExportModuleForAssertion(it.moduleName, it.artifact.name) }

private data class SwiftExportModuleForAssertion(
    val moduleName: String,
    val artifactName: String,
)

private val <T : KotlinCompilation<*>> NamedDomainObjectCollection<out T>.swiftExport: T get() = getByName("swiftExportMain")