/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.appendText
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@DisplayName("KLibs in K1")
@MppGradlePluginTests
open class KlibBasedMppIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copyEnsuringK1()

    @DisplayName("Could be compiled with project dependency")
    @GradleTest
    fun testBuildWithProjectDependency(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        testBuildWithDependency(gradleVersion, localRepo) {
            buildGradleKts.appendText(
                """
                |
                |dependencies {
                |    commonMainImplementation(project("$dependencyModuleName"))
                |}
                """.trimMargin()
            )
        }
    }

    @DisplayName("KT-36674: Could be compiled with empty source set")
    @GradleTest
    fun testPublishingAndConsumptionWithEmptySourceSet(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        testBuildWithDependency(gradleVersion, localRepo) {
            subProject(dependencyModuleName)
                .kotlinSourcesDir("windowsMain")
                .parent
                .run {
                    assertTrue(isDirectory())
                    deleteRecursively()
                }
            publishProjectDepAndAddDependency(validateHostSpecificPublication = false)
        }
    }

    @DisplayName("Compiles with common sources in transitive dependencies")
    @GradleTest
    fun testCommonSourceSetsInTransitiveDependencies(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        project(
            "common-klib-lib-and-app",
            gradleVersion,
            localRepoDir = localRepo
        ) {
            // On macOS KT-41083 is also validated by publishing a lib with host specific source sets depending on another lib with host-specific source sets
            val projectDepName = "dependency"
            val publishedGroup = "published"
            val producerProjectName = "producer"
            includeOtherProjectAsSubmodule(
                otherProjectName = "common-klib-lib-and-app",
                newSubmoduleName = projectDepName,
                isKts = true,
                localRepoDir = localRepo
            )

            subProject(projectDepName)
                .projectPath
                .allKotlinFiles
                .forEach { ktFile ->
                    // Avoid FQN duplicates between producer & consumer
                    ktFile.replaceText("package com.h0tk3y.hmpp.klib.demo", "package com.h0tk3y.hmpp.klib.lib")
                }

            subProject(projectDepName).buildGradleKts.appendText(
                """
                |
                |group = "$publishedGroup"
                """.trimMargin()
            )

            buildGradleKts.appendText(
                """
                |
                |dependencies { "commonMainImplementation"(project(":$projectDepName")) }
                |group = "$publishedGroup"
                """.trimMargin()
            )
            settingsGradleKts.appendText(
                """
                |
                |rootProject.name = "$producerProjectName"
                |
                """.trimMargin()
            )

            build("publish")

            // Then consume the published project. To do that, rename the modules so that Gradle chooses the published ones given the original
            // Maven coordinates and doesn't resolve them as project dependencies.
            val localGroup = "local"
            subProject(projectDepName).buildGradleKts.appendText(
                """
                |
                |group = "$localGroup"
                """.trimMargin()
            )
            buildGradleKts.appendText(
                """
                |
                |repositories { maven("${'$'}rootDir/repo") }
                |dependencies { "commonMainImplementation"("$publishedGroup:$producerProjectName:1.0") }
                |group = "$localGroup"
                """.trimMargin()
            )

            val commonModules = listOf(
                "published-producer-1.0-commonMain-[\\w-]+.klib",
                "published-dependency-1.0-commonMain-[\\w-]+.klib",
            ).map(::Regex)

            val hostSpecificModules = listOf(
                "published-producer-1.0-iosMain-[\\w-]+.klib",
                "published-dependency-1.0-iosMain-[\\w-]+.klib",
            ).map(::Regex)

            val windowsAndLinuxModules = listOf(
                "published-producer-1.0-windowsAndLinuxMain-[\\w-]+.klib",
                "published-dependency-1.0-windowsAndLinuxMain-[\\w-]+.klib",
            ).map(::Regex)

            checkTaskCompileClasspath(
                "compileWindowsAndLinuxMainKotlinMetadata",
                checkModulesInClasspath = commonModules + windowsAndLinuxModules,
                checkModulesNotInClasspath = hostSpecificModules
            )

            // The consumer should correctly receive the klibs of the host-specific source sets
            if (HostManager.hostIsMac) {
                checkTaskCompileClasspath(
                    "compileIosMainKotlinMetadata",
                    checkModulesInClasspath = commonModules + hostSpecificModules,
                    checkModulesNotInClasspath = windowsAndLinuxModules
                )
            }
        }
    }

    // Host-specific dependencies are only possible on macOS
    @OsCondition(
        supportedOn = [OS.MAC],
        enabledOnCI = [OS.MAC],
    )
    @DisplayName("Works with host specific dependencies")
    @GradleTest
    fun testHostSpecificBuildWithPublishedDependency(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path
    ) {
        testBuildWithDependency(gradleVersion, localRepo) {
            publishProjectDepAndAddDependency(validateHostSpecificPublication = true)
        }
    }

    @DisplayName("Works with Kotlin native transitive dependencies")
    @GradleTest
    fun testKotlinNativeImplPublishedDeps(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path
    ) {
        testKotlinNativeImplementationDependencies(gradleVersion, localRepo) {
            build(":$transitiveDepModuleName:publish", ":$dependencyModuleName:publish")

            buildGradleKts.appendText(
                """
                |
                |repositories {
                |    maven("${'$'}rootDir/repo")
                |}
                |
                |dependencies {
                |   commonMainImplementation("$MODULE_GROUP:$dependencyModuleName:1.0")
                |}
                """.trimMargin()
            )

            listOf(transitiveDepModuleName, dependencyModuleName).forEach {
                // prevent Gradle from linking the above dependency to the project:
                subProject(it).buildGradleKts.appendText(
                    """
                    |
                    |group = "com.some.other.group"
                    """.trimMargin()
                )
            }
        }
    }

    @DisplayName("Works with project dependencies containing Kotlin Native target")
    @GradleTest
    fun testKotlinNativeImplProjectDeps(
        gradleVersion: GradleVersion,
        @TempDir localRepo: Path,
    ) {
        testKotlinNativeImplementationDependencies(gradleVersion, localRepo) {
            buildGradleKts.appendText(
                """
                |
                |dependencies { 
                |    "commonMainImplementation"(project(":$dependencyModuleName")) 
                |}
                """.trimMargin()
            )
        }
    }

    @DisplayName("KT-38746: Should not disable compilation of shared source set")
    @GradleTest
    fun testAvoidSkippingSharedNativeSourceSetKt38746(gradleVersion: GradleVersion) {
        project("hierarchical-all-native", gradleVersion) {
            val targetNames = listOf(
                // Try different alphabetical ordering of the targets to ensure that the behavior doesn't depend on it,
                // as with 'first target'
                listOf("a1", "a2", "a3"),
                listOf("a3", "a1", "a2"),
                listOf("a2", "a3", "a1"),
            )
            val targetParamNames = listOf("mingwTargetName", "linuxTargetName", "macosTargetName", "currentHostTargetName")
            for (names in targetNames) {
                val currentHostTargetName = when {
                    HostManager.hostIsMingw -> names[0]
                    HostManager.hostIsLinux -> names[1]
                    HostManager.hostIsMac -> names[2]
                    else -> error("unexpected host")
                }
                val params = targetParamNames.zip(names + currentHostTargetName) { k, v -> "-P$k=$v" }
                build(":clean", ":compileCurrentHostAndLinuxKotlinMetadata", *params.toTypedArray()) {
                    assertTasksExecuted(":compileCurrentHostAndLinuxKotlinMetadata", ":compileAllNativeKotlinMetadata")
                }
            }
        }
    }

    private fun TestProject.publishProjectDepAndAddDependency(validateHostSpecificPublication: Boolean) {
        build(":$dependencyModuleName:publish") {
            if (validateHostSpecificPublication) checkPublishedHostSpecificMetadata(this@publishProjectDepAndAddDependency)
        }

        buildGradleKts.appendText(
            """
            |
            |repositories {
            |    maven("${'$'}rootDir/repo")
            |}
            |
            |dependencies {
            |   commonMainImplementation("$MODULE_GROUP:$dependencyModuleName:1.0")
            |}
            """.trimMargin()
        )

        // prevent Gradle from linking the above dependency to the project:
        subProject(dependencyModuleName)
            .buildGradleKts
            .appendText(
                """
                |
                |group = "some.other.group"
                """.trimMargin()
            )
    }

    private val dependencyModuleName = "project-dep"
    private val transitiveDepModuleName = "transitive-dep"

    private fun testBuildWithDependency(
        gradleVersion: GradleVersion,
        localRepo: Path,
        configureDependency: TestProject.() -> Unit
    ) {
        project("common-klib-lib-and-app", gradleVersion, localRepoDir = localRepo) {
            includeOtherProjectAsSubmodule(
                otherProjectName = "common-klib-lib-and-app",
                newSubmoduleName = dependencyModuleName,
                isKts = true,
                localRepoDir = localRepo,
            )

            subProject(dependencyModuleName)
                .kotlinSourcesDir("commonMain")
                .resolve("TestKt37832.kt")
                .writeText(
                    """
                    |package com.example.test.kt37832
                    |
                    |class MyException : RuntimeException()
                    """.trimMargin()
                )

            subProject(dependencyModuleName)
                .projectPath
                .allKotlinFiles
                .forEach { file ->
                    file.replaceText("package com.h0tk3y.hmpp.klib.demo", "package com.projectdep")
                }

            configureDependency()

            kotlinSourcesDir("commonMain").resolve("LibUsage.kt").writeText(
                """
                |
                |package com.h0tk3y.hmpp.klib.demo.test
                |
                |import com.projectdep.LibCommonMainExpect as ProjectDepExpect
                |
                |private fun useProjectDep() {
                |    ProjectDepExpect()
                |}
                """.trimMargin()
            )

            kotlinSourcesDir("linuxMain").resolve("LibLinuxMainUsage.kt").writeText(
                """
                |
                |package com.h0tk3y.hmpp.klib.demo.test
                |
                |import com.projectdep.libLinuxMainFun as libFun
                |
                |private fun useProjectDep() {
                |    libFun()
                |}
                """.trimMargin()
            )

            val tasksToExecute = listOfNotNull(
                ":compileJvmAndJsMainKotlinMetadata",
                ":compileLinuxMainKotlinMetadata",
                if (HostManager.hostIsMac) ":compileIosMainKotlinMetadata" else null
            )

            build("assemble") {
                assertTasksExecuted(tasksToExecute)

                assertFileInProjectExists("build/classes/kotlin/metadata/commonMain/default/manifest")
                assertFileInProjectExists("build/classes/kotlin/metadata/jvmAndJsMain/default/manifest")
                assertDirectoryInProjectExists("build/classes/kotlin/metadata/linuxMain/klib/${projectName}_linuxMain")

                // Check that the common and JVM+JS source sets don't receive the Kotlin/Native stdlib in the classpath:
                assertFalse(classpathHasKNStdlib(getClasspath(":compileCommonMainKotlinMetadata")))
                assertFalse(classpathHasKNStdlib(getClasspath(":compileJvmAndJsMainKotlinMetadata")))
            }
        }
    }

    private fun classpathHasKNStdlib(classpath: Iterable<String>) = classpath.any { "klib/common/stdlib" in it.replace("\\", "/") }

    private fun BuildResult.getClasspath(taskPath: String): Iterable<String> {
        val argsPrefix = " $taskPath Kotlin compiler args:"
        return output.lines().single { argsPrefix in it }
            .substringAfter("-classpath ").substringBefore(" -").split(File.pathSeparator)
    }

    private fun BuildResult.checkPublishedHostSpecificMetadata(project: TestProject) {
        val groupDir = project.projectPath.resolve("repo/com/example")

        assertTasksExecuted(":$dependencyModuleName:compileIosMainKotlinMetadata")

        // Check that the metadata JAR doesn't contain the host-specific source set entries, but contains the shared-Native source set
        // that can be built on every host:
        ZipFile(groupDir.resolve("$dependencyModuleName/1.0/$dependencyModuleName-1.0-all.jar").toFile())
            .use { metadataJar ->
                assertTrue(metadataJar.entries().asSequence().none { it.name.startsWith("iosMain") })
                assertTrue(metadataJar.entries().asSequence().any { it.name.startsWith("linuxMain") })
            }

        // Then check that in the host-specific modules, there's a metadata artifact that contains the host-specific source set but not the
        // common source sets:
        val hostSpecificTargets = when {
            HostManager.hostIsMac -> listOf("iosArm64", "iosX64")
            else -> error("Host doesn't support host-specific metadata")
        }

        hostSpecificTargets.forEach { targetName ->
            val moduleName = "$dependencyModuleName-${targetName.lowercase(Locale.getDefault())}"
            ZipFile(groupDir.resolve("$moduleName/1.0/$moduleName-1.0-metadata.jar").toFile())
                .use { metadataJar ->
                    assertTrue(metadataJar.entries().asSequence().any { it.name.startsWith("iosMain") })
                    assertTrue(metadataJar.entries().asSequence().none { it.name.startsWith("commonMain") })
                }
        }

        // Also check that the targets that don't include any host-specific sources don't even have the metadata artifact:
        assertFileNotExists(
            groupDir.resolve("$dependencyModuleName-linuxx64/1.0/$dependencyModuleName-linuxx64-1.0-metadata.jar")
        )
    }

    private fun testKotlinNativeImplementationDependencies(
        gradleVersion: GradleVersion,
        localRepo: Path,
        setupProject: TestProject.() -> Unit,
    ) {
        project("common-klib-lib-and-app", gradleVersion, localRepoDir = localRepo) {
            includeOtherProjectAsSubmodule(
                otherProjectName = "common-klib-lib-and-app",
                newSubmoduleName = transitiveDepModuleName,
                isKts = true,
                localRepoDir = localRepo,
            )
            includeOtherProjectAsSubmodule(
                otherProjectName = "common-klib-lib-and-app",
                newSubmoduleName = dependencyModuleName,
                isKts = true,
                localRepoDir = localRepo
            )

            subProject(dependencyModuleName)
                .projectPath
                .allKotlinFiles
                .forEach { file ->
                    // Avoid duplicate FQNs as in the compatibility mode, the K2Metadata compiler reports duplicate symbols on them:
                    file.replaceText("package com.h0tk3y.hmpp.klib.demo", "package com.h0tk3y.hmpp.klib.demo1")
                }

            subProject(dependencyModuleName)
                .buildGradleKts
                .appendText(
                    """
                    |
                    |dependencies {
                    |    "commonMainImplementation"(project(":$transitiveDepModuleName")) 
                    |}
                    """.trimMargin()
                )

            setupProject(this)

            build(":compileLinuxMainKotlinMetadata")
        }
    }

    private fun TestProject.checkTaskCompileClasspath(
        taskPath: String,
        checkModulesInClasspath: List<Regex> = emptyList(),
        checkModulesNotInClasspath: List<Regex> = emptyList(),
    ) {
        val subproject = taskPath.substringBeforeLast(":").takeIf { it.isNotEmpty() && it != taskPath }
        val taskName = taskPath.removePrefix(subproject.orEmpty())
        val taskClass = "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool<*>"
        val expression = """(tasks.getByName("$taskName") as $taskClass).libraries.toList()"""
        checkPrintedItems(subproject, expression, checkModulesInClasspath, checkModulesNotInClasspath)
    }

    private fun TestProject.checkPrintedItems(
        subproject: String?,
        itemsExpression: String,
        checkAnyItemsContains: List<Regex>,
        checkNoItemContains: List<Regex>,
    ) {
        val printingTaskName = "printItems"
        val testProject = if (subproject != null) subProject(subproject) else this
        testProject.buildGradleKts.appendText(
            """

            |tasks.register("$printingTaskName") {
            |    dependsOn("transformDependenciesMetadata")
            |    doLast {
            |        println("###$printingTaskName" + $itemsExpression)
            |    }
            |}
            """.trimMargin()
        )

        build("${subproject?.prependIndent(":").orEmpty()}:$printingTaskName") {
            val itemsLine = output.lines().single { "###$printingTaskName" in it }.substringAfter(printingTaskName)
            // NOTE: This does not work for commonized libraries, they may contain the ',' naturally
            val items = itemsLine.removeSurrounding("[", "]").split(", ").toSet()
            checkAnyItemsContains.forEach { pattern ->
                assertTrue(items.any { pattern in it }, "Couldn't find pattern `$pattern` in the output")
            }
            checkNoItemContains.forEach { pattern ->
                assertFalse(items.any { pattern in it }, "Pattern '$pattern' should NOT be present in the output")
            }
        }
    }

    companion object {
        private const val MODULE_GROUP = "com.example"
    }
}