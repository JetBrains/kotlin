/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.plugins
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.*
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportImpl
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_KMP_STRICT_RESOLVE_IDE_DEPENDENCIES
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.AssumptionViolatedException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.*
import java.util.zip.CRC32
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

@MppGradlePluginTests
@DisplayName("Multiplatform IDE dependency resolution")
class MppIdeDependencyResolutionIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions
            .disableConfigurationCache_KT70416()

    @GradleTest
    fun testCommonizedPlatformDependencyResolution(gradleVersion: GradleVersion) {
        with(project("commonizeHierarchically", gradleVersion)) {
            resolveIdeDependencies(":p1") { dependencies ->
                if (task(":p1:commonizeNativeDistribution") == null) fail("Missing :commonizeNativeDistribution task")

                fun Iterable<IdeaKotlinDependency>.filterNativePlatformDependencies() =
                    filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                        .filter { !it.isNativeStdlib }
                        .filter { it.isNativeDistribution }
                        .filter { it.binaryType == IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE }

                val nativeMainDependencies = dependencies["nativeMain"].filterNativePlatformDependencies()
                val nativeTestDependencies = dependencies["nativeTest"].filterNativePlatformDependencies()
                val linuxMainDependencies = dependencies["linuxMain"].filterNativePlatformDependencies()
                val linuxTestDependencies = dependencies["linuxTest"].filterNativePlatformDependencies()

                /* Check test and main receive the same dependencies */
                run {
                    nativeMainDependencies.assertMatches(nativeTestDependencies)
                    linuxMainDependencies.assertMatches(linuxTestDependencies)
                }

                /* Check all dependencies are marked as commonized and commonizer target match */
                run {
                    nativeMainDependencies.plus(linuxMainDependencies).forEach { dependency ->
                        if (!dependency.isCommonized) fail("$dependency is not marked as 'isCommonized'")
                    }

                    val nativeMainTarget = CommonizerTarget(
                        LINUX_X64, LINUX_ARM64, MACOS_X64, MACOS_ARM64, IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, MINGW_X64
                    )

                    nativeMainDependencies.forEach { dependency ->
                        assertEquals(nativeMainTarget.identityString, dependency.klibExtra?.commonizerTarget)
                    }

                    val linuxMainTarget = CommonizerTarget(LINUX_X64, LINUX_ARM64)
                    linuxMainDependencies.forEach { dependency ->
                        assertEquals(linuxMainTarget.identityString, dependency.klibExtra?.commonizerTarget)
                    }
                }

                /* Find posix library */
                run {
                    nativeMainDependencies.assertMatches(
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:posix:.*")),
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:.*"))
                    )

                    linuxMainDependencies.assertMatches(
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:posix:.*")),
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:.*"))
                    )
                }
            }
        }
    }

    @GradleTest
    fun testCinterops(gradleVersion: GradleVersion) {
        project(
            projectName = "cinteropImport",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
            buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()
        ) {
            build(":dep-with-cinterop:publishAllPublicationsToBuildRepository")

            resolveIdeDependencies("dep-with-cinterop") { dependencies ->
                dependencies.assertResolvedDependenciesOnly()

                dependencies["commonMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(ios_x64, linux_arm64, linux_x64\\)")))
                dependencies["commonTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(ios_x64, linux_arm64, linux_x64\\)")))
                dependencies["linuxX64Main"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_x64")))
                dependencies["linuxArm64Main"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_arm64")))
                dependencies["linuxX64Test"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_x64")))
                dependencies["linuxArm64Test"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_arm64")))

                if (HostManager.hostIsMac) {
                    dependencies["iosX64Main"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*ios_x64")))
                }
            }

            resolveIdeDependencies("client-for-binary-dep") { dependencies ->
                dependencies["commonMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))
                dependencies["commonTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))

                // CInterops are currently imported as extra roots of a platform publication, not as separate libraries
                // This is a bit inconsistent with other CInterop dependencies, but correctly represents the published artifacts
                fun assertDependencyOnPublishedProjectCInterop(sourceSetName: String, targetName: String) {
                    val publishedProjectDependencies = dependencies[sourceSetName].filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                        .filter { it.coordinates?.module?.contains("dep-with-cinterop") == true }

                    val fileNames = publishedProjectDependencies
                        .flatMap { dependency -> dependency.classpath }
                        .map { file -> file.name }
                        .toSet()

                    assert(
                        fileNames == setOf(
                            "dep-with-cinterop-${targetName}Main-1.0.klib",
                            "dep-with-cinterop-${targetName}Cinterop-depMain-1.0.klib"
                        )
                    ) {
                        """Unexpected cinterop dependencies for the source set :client-for-binary-dep:$sourceSetName.
                            |Expected a project dependency and a cinterop dependency, but instead found:
                            |$fileNames""".trimMargin()
                    }
                }

                assertDependencyOnPublishedProjectCInterop("linuxX64Main", "linuxX64")
                assertDependencyOnPublishedProjectCInterop("linuxX64Test", "linuxX64")
                assertDependencyOnPublishedProjectCInterop("linuxArm64Main", "linuxArm64")
                assertDependencyOnPublishedProjectCInterop("linuxArm64Test", "linuxArm64")
            }

            resolveIdeDependencies("client-for-project-to-project-dep") { dependencies ->
                dependencies.assertResolvedDependenciesOnly()

                dependencies["commonMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(ios_x64, linux_arm64, linux_x64\\)")))
                dependencies["commonTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(ios_x64, linux_arm64, linux_x64\\)")))

                dependencies["linuxX64Main"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_x64")))
                dependencies["linuxX64Test"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_x64")))
                dependencies["linuxArm64Main"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_arm64")))
                dependencies["linuxArm64Test"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_arm64")))

                if (HostManager.hostIsMac) {
                    dependencies["iosX64Main"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*ios_x64")))
                }
            }

            resolveIdeDependencies("client-with-complex-hierarchy") { dependencies ->
                dependencies.assertResolvedDependenciesOnly()

                dependencies["commonMain"].cinteropDependencies().assertMatches()
                dependencies["commonTest"].cinteropDependencies().assertMatches()
                dependencies["nativeMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*\\(linux_arm64, linux_x64\\)"))
                )
                dependencies["nativeTest"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*\\(linux_arm64, linux_x64\\)"))
                )

                dependencies["linuxArmMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_arm64$")))
                dependencies["linuxArmTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_arm64$")))

                dependencies["linuxIntermediateMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$")),
                )
                dependencies["linuxIntermediateTest"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$")),
                )
                dependencies["linuxMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$")),
                )
                dependencies["linuxTest"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$"))
                )
            }
        }
    }

    @GradleTest
    fun `test cinterops - are stored in project-wide persistent cache folder`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "cinteropImport",
            gradleVersion = gradleVersion,
        ) {
            resolveIdeDependencies("dep-with-cinterop") { dependencies ->

                /* Check behaviour of platform cinterops on linuxX64Main */
                val cinterops = dependencies["linuxX64Main"].filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                    .filter { !it.isNativeDistribution && it.klibExtra?.isInterop == true }
                    .ifEmpty { fail("Expected at least one cinterop on linuxX64Main") }

                val persistentCInteropsCache = projectPersistentCache.resolve("metadata").resolve("kotlinCInteropLibraries")
                cinterops.forEach { cinterop ->
                    if (cinterop.classpath.isEmpty()) fail("Missing classpath for $cinterop")
                    cinterop.classpath.forEach { cinteropFile ->
                        /* Check file was copied into root .gradle folder */
                        assertEquals(persistentCInteropsCache.toFile().canonicalPath, cinteropFile.parentFile.canonicalPath)

                        /* Check crc in file name */
                        val crc = CRC32()
                        crc.update(cinteropFile.readBytes())
                        val crcValue = crc.value.toInt()
                        val crcString = Base64.getUrlEncoder().withoutPadding().encodeToString(
                            ByteBuffer.allocate(4).putInt(crcValue).array()
                        )

                        if (!cinteropFile.name.endsWith("-$crcString.klib")) {
                            fail("Expected crc $crcString to be part of cinterop file name. Found ${cinteropFile.name}")
                        }
                    }
                }
            }
        }
    }

    @GradleTest
    fun `test cinterops - with failing cinterop process`(gradleVersion: GradleVersion) {
        project(
            projectName = "cinterop-withFailingCInteropProcess", gradleVersion = gradleVersion,
            /* Adding idea.sync.active to ensure lenient cinterop generation */
            buildOptions = defaultBuildOptions.copy(freeArgs = listOf("-Didea.sync.active=true"))
        ) {
            resolveIdeDependencies { dependencies ->
                dependencies["commonMain"].assertMatches(
                    kotlinNativeDistributionDependencies,
                    binaryCoordinates(Regex("com.example:cinterop-.*-dummy:linux_x64")),
                    IdeaKotlinDependencyMatcher("Unresolved 'failing' cinterop") { dependency ->
                        dependency is IdeaKotlinUnresolvedBinaryDependency && dependency.cause.orEmpty().contains(
                            "cinterop-withFailingCInteropProcess-linuxX64Cinterop-failing"
                        )
                    }
                )
            }
        }
    }

    @GradleTest
    fun `test cinterops - commonized interop name should include targets unsupported on host`(gradleVersion: GradleVersion) {
        if (HostManager.hostIsMac) {
            throw AssumptionViolatedException("Host shouldn't support ios target")
        }

        project("cinterop-ios", gradleVersion) {
            resolveIdeDependencies { dependencies ->
                dependencies["commonMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("""a:cinterop-ios-cinterop-myinterop.*\(ios_x64, linux_x64\)"""))
                )
            }
        }
    }

    @GradleTest
    fun `test cinterops - transitive project dependencies`(gradleVersion: GradleVersion) {
        project(
            "cinterop-MetadataDependencyTransformation",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(freeArgs = defaultBuildOptions.freeArgs + "-PdependencyMode=project")
        ) {
            resolveIdeDependencies(":p3") { dependencies ->
                /* Check that no compile-tasks are executed */
                assertNoCompileTasksGotExecuted()

                dependencies["nativeMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex(".*p1-cinterop-simple.*")),
                    binaryCoordinates(Regex(".*p1-cinterop-withPosix.*"))
                )

                dependencies["linuxX64Main"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex(".*p1-cinterop-simple.*")),
                    binaryCoordinates(Regex(".*p1-cinterop-withPosix.*"))
                )
            }
        }
    }

    @GradleTest
    fun `test cinterops - transitive project dependencies - disabled cinterop commonization`(gradleVersion: GradleVersion) {
        project(
            "cinterop-MetadataDependencyTransformation",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                freeArgs = defaultBuildOptions.freeArgs + "-PdependencyMode=project" + "-Pkotlin.mpp.enableCInteropCommonization=false"
            )
        ) {
            resolveIdeDependencies(":p3") { dependencies ->
                dependencies["nativeMain"].cinteropDependencies().assertMatches()
                dependencies["linuxX64Main"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex(".*p1-cinterop-simple.*")),
                    binaryCoordinates(Regex(".*p1-cinterop-withPosix.*"))
                )
            }
        }
    }

    @GradleTest
    fun `test dependency on composite build with commonized cinterops`(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val includedLib = project("composite-build-with-cinterop-commonization/includedLib", gradleVersion, localRepoDir = tempDir)
        project("composite-build-with-cinterop-commonization", gradleVersion, localRepoDir = tempDir) {
            settingsGradleKts.replaceText("<includedLib_path>", includedLib.projectPath.toUri().toString())
            // Quick fix for: KT-58815
            build(":lib:copyCommonizeCInteropForIde")
            resolveIdeDependencies { dependencies ->
                dependencies["linuxMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates("org.example:included-lib-cinterop-a:(linux_arm64, linux_x64)"),
                    binaryCoordinates("org.example:lib-cinterop-a:(linux_arm64, linux_x64)"),
                )
            }
        }
    }

    @GradleTest
    fun `test dependency on java testFixtures and feature source sets`(gradleVersion: GradleVersion) {
        project(
            "kt-60053-dependencyOn-testFixtures",
            gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion)
        ) {
            build("publish")

            resolveIdeDependencies(":consumer") { dependencies ->
                val jvmMainDependencies = dependencies["jvmMain"].filterIsInstance<IdeaKotlinBinaryDependency>().assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    binaryCoordinates("org.jetbrains.sample:producer:1.0.0"),
                    binaryCoordinates("org.jetbrains.sample:producer-foo:1.0.0"),
                )

                val jvmTestDependencies = dependencies["jvmTest"].filterIsInstance<IdeaKotlinBinaryDependency>().assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    binaryCoordinates("org.jetbrains.sample:producer:1.0.0"),
                    binaryCoordinates("org.jetbrains.sample:producer-foo:1.0.0"),
                    binaryCoordinates("org.jetbrains.sample:producer-test-fixtures:1.0.0"),
                )

                jvmMainDependencies.getOrFail(binaryCoordinates("org.jetbrains.sample:producer:1.0.0")).assertSingleSourcesJar()
                jvmTestDependencies.getOrFail(binaryCoordinates("org.jetbrains.sample:producer:1.0.0")).assertSingleSourcesJar()
            }
        }
    }

    @GradleTest
    fun `test native project dependencies resolve leniently`(gradleVersion: GradleVersion) {
        project("kt-61466-lenient-dependency-resolution", gradleVersion) {
            resolveIdeDependencies(":consumer") { dependencies ->
                dependencies["commonMain"].assertMatches(
                    kotlinNativeDistributionDependencies,
                )

                dependencies["linuxMain"].assertMatches(
                    kotlinNativeDistributionDependencies,
                    dependsOnDependency(":consumer/commonMain"),
                    dependsOnDependency(":consumer/nativeMain")
                )

                dependencies["linuxX64Main"].assertMatches(
                    kotlinNativeDistributionDependencies,
                    dependsOnDependency(":consumer/commonMain"),
                    dependsOnDependency(":consumer/nativeMain"),
                    dependsOnDependency(":consumer/linuxMain"),
                    binaryCoordinates("this:does:not-exist"),
                    IdeaKotlinDependencyMatcher("Project Dependency: producer") { dependency ->
                        dependency is IdeaKotlinUnresolvedBinaryDependency && ":producer" in dependency.cause.orEmpty()
                    }
                )
            }
        }
    }

    @GradleTest
    fun `kt-61652 test no CME when jupiter plugin is applied to independet project`(gradleVersion: GradleVersion) {
        project("kt-61652-CME-when-jupiter-is-applied-to-independet-project", gradleVersion) {
            resolveIdeDependencies(":app") {
                assertOutputDoesNotContain("ConcurrentModificationException")
            }
        }
    }

    @GradleTest
    fun `test resolve sources for dependency with multiple capabilities`(gradleVersion: GradleVersion) {
        project(
            "kt-63226-multiple-capabilities",
            gradleVersion,
            localRepoDir = workingDir.resolve(gradleVersion.version),
        ) {
            build(":producer:publish")

            resolveIdeDependencies(":consumer") { dependencies ->
                dependencies["jvmMain"].getOrFail(
                    binaryCoordinates("test:producer:1.0")
                        .withResolvedSourcesFile("producer-1.0-sources.jar")
                )

                // according to build.gradle.kts of test project jvmTest should have both artifacts from :producer
                // one with regular capabilities
                dependencies["jvmTest"].getOrFail(
                    binaryCoordinates("test:producer:1.0")
                        .withResolvedSourcesFile("producer-1.0-sources.jar")
                )

                // and another with foo capability
                dependencies["jvmTest"].getOrFail(
                    binaryCoordinates("test:producer:1.0")
                        .withResolvedSourcesFile("producer-1.0-foo-sources.jar")
                )
            }
        }
    }

    @GradleTest
    fun `test resolve sources for transitive dependencies through dependency without sources variant`(gradleVersion: GradleVersion) {
        project(
            "transitive-sources-dependencies",
            gradleVersion,
            localRepoDir = workingDir.resolve(gradleVersion.version),
        ) {
            // lib_without_sources dependsOn lib_with_sources
            build(":lib_with_sources:publish", ":lib_without_sources:publish")

            settingsGradleKts.replaceText("val isConsumer = false", "val isConsumer = true")

            resolveIdeDependencies(":consumer") { dependencies ->
                dependencies["jvmMain"].getOrFail(
                    binaryCoordinates("test:lib_with_sources-jvm:1.0")
                        .withResolvedSourcesFile("lib_with_sources-jvm-1.0-sources.jar"),
                )

                dependencies["jvmMain"].getOrFail(
                    binaryCoordinates("test:lib_without_sources-jvm:1.0")
                ).assertNoSourcesResolved()

                dependencies["linuxX64Main"].getOrFail(
                    binaryCoordinates("test:lib_with_sources-linuxx64:1.0")
                        .withResolvedSourcesFile("lib_with_sources-linuxx64-1.0-sources.jar"),
                )

                dependencies["linuxX64Main"].getOrFail(
                    binaryCoordinates("test:lib_without_sources-linuxx64:1.0")
                ).assertNoSourcesResolved()
            }
        }
    }

    @GradleTest
    fun `KT-71074 jvmMain depends on kotlin jvm project`(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-jvm-library", newSubmoduleName = "jvm")

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    linuxX64()

                    sourceSets.jvmMain.dependencies {
                        api(project(":jvm"))
                    }
                }
            }

            resolveIdeDependencies { dependencies ->
                assertNoCompileTasksGotExecuted()
                dependencies.assertResolvedDependenciesOnly()

                dependencies["jvmMain"].getOrFail(
                    projectArtifactDependency(
                        Regular,
                        ":jvm",
                        FilePathRegex(".*/jvm.jar")
                    )
                )
            }
        }
    }

    @GradleTest
    fun `KT-74727 intermediate platform-specific source set with project dependency`(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "kmp-lib") {
                buildScriptInjection {
                    kotlinMultiplatform.apply {
                        jvm()
                        linuxX64()
                    }
                }
            }

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    sourceSets.commonMain.dependencies { // since only jvm is declared commonMain here is JVM-specific source set
                        api(project(":kmp-lib"))
                    }
                }
            }

            resolveIdeDependencies { dependencies ->
                assertNoCompileTasksGotExecuted()
                dependencies.assertResolvedDependenciesOnly()

                val expectedJvmDependencies = listOf(
                    jetbrainsAnnotationDependencies,
                    kotlinStdlibDependencies,
                    // FIXME: KT-74782 This is technically a bug, as we should expect that "kmp-lib" would be resolved
                    //  as bunch of regular source dependencies. i.e. :kmp-lib:commonMain and :kmp-lib:jvmMain
                    //  but IDEA is smart enough to convert this projectArtifactDependency to beforementioned source dependencies
                    projectArtifactDependency(
                        Regular,
                        ":kmp-lib",
                        FilePathRegex(".*/kmp-lib-jvm.jar")
                    )
                )
                dependencies["commonMain"]
                    .assertMatches(expectedJvmDependencies)
                dependencies["jvmMain"]
                    .assertMatches(expectedJvmDependencies + dependsOnDependency(":/commonMain"))

                val friendDependencies = listOf(
                    friendSourceDependency(":/commonMain"),
                    friendSourceDependency(":/jvmMain"),
                )

                dependencies["commonTest"]
                    .assertMatches(expectedJvmDependencies + friendDependencies)
                dependencies["jvmTest"]
                    .assertMatches(expectedJvmDependencies + friendDependencies + dependsOnDependency(":/commonTest") )
            }
        }
    }

    @GradleTest
    fun `KT-75605 dependency to kmp project from test source set`(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            includeOtherProjectAsSubmodule("base-kotlin-multiplatform-library", newSubmoduleName = "test-utils") {
                buildScriptInjection {
                    kotlinMultiplatform.apply {
                        jvm()
                        linuxX64()
                        linuxArm64()
                        iosX64()
                    }
                }
            }

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    linuxX64()
                    linuxArm64()
                    iosX64()

                    sourceSets.commonTest.dependencies {
                        api(project(":test-utils"))
                    }
                }
            }

            resolveIdeDependencies { dependencies ->
                assertNoCompileTasksGotExecuted()
                dependencies.assertResolvedDependenciesOnly()

                dependencies["commonTest"].assertMatches(
                    friendSourceDependency(":/commonMain"),
                    regularSourceDependency(":test-utils/commonMain"),
                    kotlinStdlibDependencies
                )
                dependencies["nativeTest"].assertMatches(
                    friendSourceDependency(":/commonMain"),
                    friendSourceDependency(":/nativeMain"),
                    dependsOnDependency(":/commonTest"),
                    regularSourceDependency(":test-utils/commonMain"),
                    regularSourceDependency(":test-utils/nativeMain"),
                    kotlinNativeDistributionDependencies, // kotlin-stdlib is part of native distribution
                )
            }
        }
    }

    @OptIn(ExternalKotlinTargetApi::class)
    @GradleTest
    fun `IDE resolution strict mode`(gradleVersion: GradleVersion) {
        class Foo : Exception()
        class Bar : Exception()
        val baz = "baz"
        val project = project("empty", gradleVersion) {
            plugins { kotlin("multiplatform") }
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                project.kotlinIdeMultiplatformImport.registerDependencyResolver(
                    resolver = IdeDependencyResolver { throw Foo() },
                    constraint = IdeMultiplatformImport.SourceSetConstraint.unconstrained,
                    phase = IdeMultiplatformImport.DependencyResolutionPhase.PreDependencyResolution,
                )
                project.kotlinIdeMultiplatformImport.registerDependencyResolver(
                    resolver = IdeDependencyResolver { throw Bar() },
                    constraint = IdeMultiplatformImport.SourceSetConstraint.unconstrained,
                    phase = IdeMultiplatformImport.DependencyResolutionPhase.SourcesAndDocumentationResolution,
                )
                project.kotlinIdeMultiplatformImport.registerDependencyResolver(
                    resolver = IdeDependencyResolver {
                        (project.kotlinIdeMultiplatformImport as IdeMultiplatformImportImpl).importLogger.warn(baz)
                        emptySet()
                    },
                    constraint = IdeMultiplatformImport.SourceSetConstraint.unconstrained,
                    phase = IdeMultiplatformImport.DependencyResolutionPhase.SourceDependencyResolution,
                )
            }
        }
        project.resolveIdeDependencies(strictMode = false) {
            assertOutputContains("e: org.jetbrains.kotlin.gradle.plugin.ide")
        }
        assertThrows<Exception> { project.resolveIdeDependencies(strictMode = true) {} }

        val events = project.catchBuildFailures<org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportLogger.Events>().buildAndReturn(
            ":resolveIdeDependencies", "-P${KOTLIN_KMP_STRICT_RESOLVE_IDE_DEPENDENCIES}=true"
        ).unwrap().single()
        assertEquals<List<Class<*>>>(
            listOf(
                Foo::class.java,
                Bar::class.java,
            ),
            events.errors.map { it.cause!!.javaClass }
        )
        assertEquals(
            listOf(baz),
            events.warnings.map { it.message }
        )
    }

    @GradleTest
    fun `KT-77414 detached source sets don't fail IDE resolution`(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            val producer = project("empty", gradleVersion) {
                plugins { kotlin("multiplatform") }
                buildScriptInjection { kotlinMultiplatform.jvm() }
            }
            val producerName = "producer"
            include(producer, producerName)

            plugins { kotlin("multiplatform") }
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.sourceSets.create("detached").dependencies {
                    implementation(project(":${producerName}"))
                }
            }
        }.resolveIdeDependencies(strictMode = true) {}
    }

    @GradleAndroidTest
    fun `KT-77404 jvm+android commonTest sees stdlib and annotations`(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "base-kotlin-multiplatform-android-library",
            gradleVersion,
            buildJdk = jdkVersion.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion).suppressWarningFromAgpWithGradle813(gradleVersion),
        ) {
            buildScriptInjection {
                applyDefaultAndroidLibraryConfiguration()

                kotlinMultiplatform.jvm()
                kotlinMultiplatform.androidTarget()
            }
        }.resolveIdeDependencies() { dependencies ->
            dependencies["commonMain"].assertMatches(
                kotlinStdlibDependencies,
                jetbrainsAnnotationDependencies,
            )
            dependencies["commonTest"].assertMatches(
                kotlinStdlibDependencies,
                jetbrainsAnnotationDependencies,
                friendSourceDependency(":/commonMain")
            )
        }
    }

    private fun Iterable<IdeaKotlinDependency>.cinteropDependencies() =
        this.filterIsInstance<IdeaKotlinBinaryDependency>().filter {
            it.klibExtra?.isInterop == true && !it.isNativeStdlib && !it.isNativeDistribution
        }

    private fun IdeaKotlinBinaryDependency.assertSingleSourcesJar(): File {
        val sources = sourcesClasspath.toList()
        if (sources.isEmpty()) fail("Missing -sources.jar")
        if (sources.size > 1) fail("Multiple -sources.jar: $sources")

        return sources.single().also { sourcesFile ->
            if (!sourcesFile.name.endsWith("-sources.jar")) fail("-sources.jar suffix expected. Found: ${sourcesFile.name}")
        }
    }
}

private fun BuildResult.assertNoCompileTasksGotExecuted() {
    val compileTaskRegex = Regex(".*[cC]ompile.*")
    val compileTasks = tasks.filter { task -> task.path.matches(compileTaskRegex) }
    if (compileTasks.isNotEmpty()) {
        fail("Expected no compile tasks to be executed. Found $compileTasks")
    }
}
