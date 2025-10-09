/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportConstants
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
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.junit.Assume
import org.junit.Test
import kotlin.test.*

class SwiftExportUnitTests {
    @BeforeTest
    fun runOnMacOSOnly() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
    }

    @Test
    fun `test swift export resolvable configuration is present with apple targets`() {
        with(buildProjectWithMPP()) {
            kotlin {
                iosArm64()
            }
            configureRepositoriesForTests()
            evaluate()

            val swiftExportClasspath = configurations.findByName("swiftExportClasspath")
            val swiftExportClasspathResolvable = configurations.findByName("swiftExportClasspathResolvable")

            assertNotNull(swiftExportClasspath)
            assertNotNull(swiftExportClasspathResolvable)
            assertTrue(swiftExportClasspathResolvable.isCanBeResolved, "configuration should be resolvable")
        }
    }

    @Test
    fun `test swift export resolvable configuration is not present without apple targets`() {
        with(buildProjectWithMPP()) {
            kotlin {
                linuxX64()
            }
            configureRepositoriesForTests()
            evaluate()

            val swiftExportClasspath = configurations.findByName("swiftExportClasspath")
            val swiftExportClasspathResolvable = configurations.findByName("swiftExportClasspathResolvable")

            assertNull(swiftExportClasspath)
            assertNull(swiftExportClasspathResolvable)
        }
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
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            listOf(iosSimulatorArm64(), iosX64(), iosArm64())
        })

        project.evaluate()

        val embedSwiftExportTask = project.tasks.getByName("embedSwiftExportForXcode")

        val buildType = embedSwiftExportTask.inputs.properties["type"] as NativeBuildType

        val arm64SimLib = project.multiplatformExtension.iosSimulatorArm64().binaries.findStaticLib("SwiftExportBinary", buildType)
        val arm64Lib = project.multiplatformExtension.iosArm64().binaries.findStaticLib("SwiftExportBinary", buildType)
        @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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

        val linkTask = project.tasks.getByName("linkSwiftExportBinaryDebugStaticIosSimulatorArm64") as KotlinNativeLink
        val projectLibraries = linkTask.libraries
            .filter { it.name.contains("stdlib").not() }
            .filter { it.name.contains("nativeDependencies").not() }

        val mainProjectLibrary = project.layout.buildDirectory
            .file("classes/kotlin/iosSimulatorArm64/main/klib/shared").get().asFile

        val subProject1Library = subProject1.layout.buildDirectory
            .file("classes/kotlin/iosSimulatorArm64/main/klib/subproject").get().asFile

        val subProject2Library = subProject2.layout.buildDirectory
            .file("classes/kotlin/iosSimulatorArm64/main/klib/anotherproject").get().asFile

        val subProject3Library = subProject3.layout.buildDirectory
            .file("classes/kotlin/iosSimulatorArm64/main/klib/miniproject").get().asFile

        assertEquals(
            hashSetOf(
                mainProjectLibrary,
                subProject1Library,
                subProject2Library,
                subProject3Library
            ),
            projectLibraries.files
        )
    }

    @Test
    fun `test swift export exported modules`() {
        val projects = multiModuleSwiftExportProject {
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }

        projects.forEach { it.evaluate() }
        val project = projects.first()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "Subproject",
                    "subproject",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
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
                    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxDatetime",
                    "kotlinx-datetime.klib",
                    true
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.filter { it.shouldBeFullyExported }.toModulesForAssertion(),
        )

        val KotlinxIoCore = actualModules.single { it.moduleName == "OrgJetbrainsKotlinxKotlinxIoCore" }
        assertFalse(KotlinxIoCore.shouldBeFullyExported, "Compilation dependency kotlinx-io-core should not be exported")
    }

    @Test
    fun `test transitive dependencies of exported dependencies are not exported`() {
        val project = swiftExportProject(
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList()).filter { it.shouldBeFullyExported }

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(SwiftExportModuleForAssertion("OrgJetbrainsKotlinxKotlinxDatetime", "kotlinx-datetime.klib", true))
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.singleOrNull { it.moduleName == "OrgJetbrainsKotlinxKotlinxCoroutinesCore" }
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
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList()).filter { it.shouldBeFullyExported }

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    true
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.single()
        assertContains("/1.9.0/", kotlinXCoroutines.artifact.path)
        assertNotContains("/1.8.0/", kotlinXCoroutines.artifact.path)
    }

    @Test
    fun `test compile dependency version is greater than exported dependency`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList()).filter { it.shouldBeFullyExported }

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    true
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.single()
        assertContains("/1.9.0/", kotlinXCoroutines.artifact.path)
        assertNotContains("/1.8.0/", kotlinXCoroutines.artifact.path)
    }

    @Test
    fun `test exported dependency version undefined`() {
        val project = swiftExportProject(
            multiplatform = {
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList()).filter { it.shouldBeFullyExported }

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    true
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )

        val kotlinXCoroutines = actualModules.single()
        assertContains("/1.9.0/", kotlinXCoroutines.artifact.path)
    }

    @Test
    fun `test dependency export custom parameters`() {
        val project = swiftExportProject(
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2") {
                    moduleName.set("CustomDateTime")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "CustomDateTime",
                    "kotlinx-datetime.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxSerializationCore",
                    "kotlinx-serialization-core.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test swift export custom settings`() {
        val customSettings = mapOf("SWIFT_EXPORT_CUSTOM_SETTING" to "CUSTOM_VALUE")
        val project = swiftExportProject {
            configure {
                settings.set(customSettings)
            }
        }
        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val taskSettings = swiftExportTask.parameters.swiftExportSettings.get()

        assertEquals(taskSettings, customSettings)
    }

    @Test
    fun `test swift export invalid module name`() {
        val project = swiftExportProject {
            moduleName.set("Shared.Module")
        }
        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportInvalidModuleName)
    }

    @Test
    fun `test swift export invalid exported module name`() {
        val project = swiftExportProject {
            export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2") {
                moduleName.set("Custom.DateTime")
            }
        }
        project.evaluate()

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportInvalidModuleName)
    }

    @Test
    fun `test swift export custom compiler options`() {
        val project = swiftExportProject(
            swiftExport = {
                configure {
                    freeCompilerArgs.add("-opt-in=some.value")
                }
            }
        )
        project.evaluate()

        val embedSwiftExportTask = project.tasks.getByName("embedSwiftExportForXcode")
        val buildType = embedSwiftExportTask.inputs.properties["type"] as NativeBuildType
        val arm64SimLib = project.multiplatformExtension.iosSimulatorArm64().binaries.findStaticLib(
            SwiftExportConstants.SWIFT_EXPORT_BINARY,
            buildType
        )

        assertNotNull(arm64SimLib)
        assertEquals(arm64SimLib.freeCompilerArgs.single(), "-opt-in=some.value")

        val linkTask = project.tasks.getByName("linkSwiftExportBinaryDebugStaticIosSimulatorArm64") as KotlinNativeLink
        assertEquals(arm64SimLib, linkTask.binary)
    }

    @Test
    fun `test swift export invalid project name`() {
        val invalidName = "invalid!name"
        val project = swiftExportProject(
            projectBuilder = {
                withName(invalidName)
            }
        )
        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val name = swiftExportTask.mainModuleInput.moduleName.get()
        assertEquals(invalidName.uppercaseFirstChar(), name)

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportInvalidModuleName)
    }

    @Test
    fun `test swift export invalid project name but valid module name`() {
        val validName = "SharedModule"
        val project = swiftExportProject(
            projectBuilder = {
                withName("invalid!name")
            }
        ) {
            moduleName.set(validName)
        }
        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val name = swiftExportTask.mainModuleInput.moduleName.get()
        assertEquals(validName, name)

        project.assertNoDiagnostics(KotlinToolingDiagnostics.SwiftExportInvalidModuleName)
    }

    @Test
    fun `test exporting transitive dependency was not fully exported`() {
        val project = swiftExportProject(
            projectBuilder = {
                withName("shared")
            },
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting jvm dependency`() {
        val project = swiftExportProject(
            projectBuilder = {
                withName("shared")
            },
            swiftExport = {
                export("org.glassfish:jakarta.json:2.0.1")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        assertTrue(actualModules.isEmpty(), "No modules should be exported for JVM dependencies")
    }

    @Test
    fun `test exporting invalid dependency`() {
        val project = swiftExportProject(
            projectBuilder = {
                withName("shared")
            },
            swiftExport = {
                // Invalid dependency that does not exist
                export("org.jetbrains.kotlinx:kotlinx-dateetime:0.6.2")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        project.assertContainsDiagnostic(KotlinToolingDiagnostics.SwiftExportModuleResolutionError)
        assertTrue(actualModules.isEmpty(), "No modules should be exported for invalid dependencies")
    }

    @Test
    fun `test exporting explicit dependency was fully exported`() {
        val project = swiftExportProject(
            projectBuilder = {
                withName("shared")
            },
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting both explicit and transitive dependencies`() {
        val project = swiftExportProject(
            projectBuilder = {
                withName("shared")
            },
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                }
            },
            swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxDatetime",
                    "kotlinx-datetime.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxSerializationJson",
                    "kotlinx-serialization-json-iosSimulatorArm64Main-1.8.1.klib",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxSerializationCore",
                    "kotlinx-serialization-core-iosSimulatorArm64Main-1.8.1.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting transitive dependencies in subprojects`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject", {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        })
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                }
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "SharedSubproject",
                    "subproject",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting subprojects transitive dependency not exported`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                }
            },
            swiftExport = {
                export(projectDependency)
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "Subproject",
                    "subproject",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting transitive dependencies with different versions (dependency in subproject has greater version)`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                }
            }, swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core-iosSimulatorArm64Main-1.10.0.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "SharedSubproject",
                    "subproject",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting transitive dependencies with different versions (dependency in subproject has lower version)`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        val projectDependency = project.subProject("subproject") {
            iosSimulatorArm64()
            sourceSets.commonMain.dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
                sourceSets.commonMain.dependencies {
                    implementation(projectDependency)
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
                }
            }, swiftExport = {
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            }
        )

        project.evaluate()
        projectDependency.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core-iosSimulatorArm64Main-1.10.0.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "SharedSubproject",
                    "subproject",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting different types of dependencies`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )
        project.subProject("subproject1") {
            iosSimulatorArm64()
        }
        project.subProject("subproject2") {
            iosSimulatorArm64()
        }

        val projectDependency_1 = project.project(":subproject1")
        val projectDependency_2 = project.dependencies.project(":subproject2")
        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
            },
            swiftExport = {
                export(projectDependency_1)
                export(projectDependency_2)
                export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
            }
        )

        project.evaluate()
        projectDependency_1.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core-iosSimulatorArm64Main-1.10.0.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "Subproject1",
                    "subproject1",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "Subproject2",
                    "subproject2",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }

    @Test
    fun `test exporting two runtime modules`() {
        val project = buildProject(
            projectBuilder = {
                withName("shared")
            },
            configureProject = {
                configureRepositoriesForTests()
            }
        )

        project.setupForSwiftExport(
            multiplatform = {
                iosSimulatorArm64()
            },
            swiftExport = {
                export("app.cash.sqldelight:runtime:2.1.0")
                export("org.jetbrains.compose.runtime:runtime:1.8.2")
            }
        )

        project.evaluate()

        val swiftExportTask = project.tasks.withType(SwiftExportTask::class.java).single()
        val actualModules = swiftExportTask.parameters.swiftModules.getOrElse(emptyList())

        val expectedModules = SmartSet.create<SwiftExportModuleForAssertion>().apply {
            add(
                SwiftExportModuleForAssertion(
                    "AppCashSqldelightRuntime",
                    "runtime.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsComposeRuntimeRuntime",
                    "runtime-uikitSimArm64Main-1.8.2.klib",
                    true
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxAtomicfu",
                    "atomicfu.klib",
                    false
                )
            )
            add(
                SwiftExportModuleForAssertion(
                    "OrgJetbrainsKotlinxKotlinxCoroutinesCore",
                    "kotlinx-coroutines-core.klib",
                    false
                )
            )
        }

        assertEquals(
            expectedModules,
            actualModules.toModulesForAssertion(),
        )
    }
}

private fun multiModuleSwiftExportProject(
    mainProjectName: String = "shared",
    subprojects: List<String> = listOf("subproject"),
    multiplatform: KotlinMultiplatformExtension.() -> Unit = { iosSimulatorArm64() },
    swiftExport: SwiftExportExtension.() -> Unit = {},
): List<ProjectInternal> {
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
        swiftExport()
    }

    return listOf(project) + projectDependencies
}

private fun swiftExportProject(
    configuration: String = "DEBUG",
    sdk: String = "iphonesimulator",
    archs: String = "arm64",
    projectBuilder: ProjectBuilder.() -> Unit = { },
    multiplatform: KotlinMultiplatformExtension.() -> Unit = {
        iosSimulatorArm64()
    },
    swiftExport: SwiftExportExtension.() -> Unit = {},
): ProjectInternal = buildProjectWithMPP(
    projectBuilder = projectBuilder,
    preApplyCode = {
        applyEmbedAndSignEnvironment(
            configuration = configuration,
            sdk = sdk,
            archs = archs,
        )
        configureRepositoriesForTests()
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

private fun List<SwiftExportedModule>.toModulesForAssertion() = mapToSetOrEmpty { module ->
    SwiftExportModuleForAssertion(
        module.moduleName,
        module.artifact.name,
        module.shouldBeFullyExported
    )
}

private data class SwiftExportModuleForAssertion(
    val moduleName: String,
    val artifactName: String,
    val shouldBeFullyExported: Boolean,
)

private val <T : KotlinCompilation<*>> NamedDomainObjectCollection<out T>.swiftExport: T get() = getByName("swiftExportMain")