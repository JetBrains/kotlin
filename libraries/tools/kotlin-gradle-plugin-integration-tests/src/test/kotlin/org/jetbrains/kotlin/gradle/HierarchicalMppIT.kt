/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.internals.parseKotlinSourceSetMetadataFromJson
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@MppGradlePluginTests
@DisplayName("Hierarchical multiplatform")
open class HierarchicalMppIT : KGPBaseTest() {

    private val String.withPrefix get() = "hierarchical-mpp-published-modules/$this"

    @GradleTest
    @DisplayName("Check build with published third-party library")
    fun testPublishedModules(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)

        publishThirdPartyLib(withGranularMetadata = false, gradleVersion = gradleVersion, localRepoDir = tempDir)

        nativeProject(
            "my-lib-foo".withPrefix,
            gradleVersion,
            localRepoDir = tempDir,
            buildOptions = buildOptions
        ).run {
            build("publish") {
                checkMyLibFoo(localRepoDir = tempDir)
            }
        }

        nativeProject(
            "my-lib-bar".withPrefix,
            gradleVersion,
            localRepoDir = tempDir,
            buildOptions = buildOptions
        ).run {
            build("publish") {
                checkMyLibBar(localRepoDir = tempDir)
            }
        }

        nativeProject(
            "my-app".withPrefix,
            gradleVersion,
            localRepoDir = tempDir,
            buildOptions = buildOptions
        ).run {
            build("assemble") {
                checkMyApp()
            }
        }
    }

    @GradleTest
    @DisplayName("Check no sourceSets visible if no variant matched")
    fun testNoSourceSetsVisibleIfNoVariantMatched(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir)

        nativeProject(
            projectName = "my-lib-foo".withPrefix,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ).run {
            // --- Move the dependency from jvmAndJsMain to commonMain, where there's a linuxX64 target missing in the lib
            buildGradleKts.modify {
                it.checkedReplace("api(\"com.example.thirdparty:third-party-lib:1.0\")", "//") + "\n" + """
                dependencies {
                    "commonMainApi"("com.example.thirdparty:third-party-lib:1.0")
                }
                """.trimIndent()
            }

            testDependencyTransformations { reports ->
                val thirdPartyLibApiVisibility = reports.filter { report ->
                    report.groupAndModule.startsWith("com.example.thirdparty:third-party-lib") && report.scope == "api"
                }
                val jvmJsSourceSets = setOf("jvmAndJsMain", "jvmAndJsTest")
                thirdPartyLibApiVisibility.forEach {
                    if (it.sourceSetName in jvmJsSourceSets)
                        assertTrue("$it") { it.allVisibleSourceSets == setOf("commonMain") }
                }
            }
        }
    }

    @GradleTest
    @DisplayName("Dependencies in tests should be correct with third-party library")
    fun testDependenciesInTests(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir) {
            kotlinSourcesDir("jvmMain").copyRecursively(kotlinSourcesDir("linuxX64Main"))
            buildGradleKts.appendText("\nkotlin.linuxX64()")
        }

        nativeProject(
            projectName = "my-lib-foo".withPrefix,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ).run {
            testDependencyTransformations { reports ->
                val testApiTransformationReports =
                    reports.filter { report ->
                        report.groupAndModule.startsWith("com.example.thirdparty:third-party-lib") &&
                                report.sourceSetName.let { it == "commonTest" || it == "jvmAndJsTest" } &&
                                report.scope == "api"
                    }

                testApiTransformationReports.forEach {
                    if (it.sourceSetName == "commonTest")
                        assertTrue("$it") { it.isExcluded } // should not be visible in commonTest
                    else {
                        assertTrue("$it") { it.allVisibleSourceSets == setOf("commonMain") }
                        assertTrue("$it") { it.newVisibleSourceSets == emptySet<String>() }
                    }
                }

                // ALso check that the files produced by dependency transformations survive a clean build:
                val existingFilesFromReports = reports.flatMap { it.useFiles }.filter { it.isFile }
                assertTrue { existingFilesFromReports.isNotEmpty() }
                build("clean") {
                    existingFilesFromReports.forEach { assertTrue("Expected that $it exists after clean build.") { it.isFile } }
                }
            }

            // --- Move the dependency from jvmAndJsMain to commonMain, expect that it is now propagated to commonTest:
            buildGradleKts.modify {
                it.checkedReplace("api(\"com.example.thirdparty:third-party-lib:1.0\")", "//") + "\n" + """
                dependencies {
                    "commonMainApi"("com.example.thirdparty:third-party-lib:1.0")
                }
                """.trimIndent()
            }

            testDependencyTransformations { reports ->
                val testApiTransformationReports =
                    reports.filter { report ->
                        report.groupAndModule.startsWith("com.example.thirdparty") &&
                                report.sourceSetName.let { it == "commonTest" || it == "jvmAndJsTest" } &&
                                report.scope == "api"
                    }

                testApiTransformationReports.forEach {
                    assertEquals(setOf("commonMain"), it.allVisibleSourceSets, "$it")
                    assertEquals(emptySet(), it.newVisibleSourceSets, "$it")
                }
            }

            // --- Remove the dependency from commonMain, add it to commonTest to check that it is correctly picked from a non-published
            // source set:
            buildGradleKts.modify {
                it.checkedReplace("\"commonMainApi\"(\"com.example.thirdparty:third-party-lib:1.0\")", "//") + "\n" + """
                dependencies {
                    "commonTestApi"("com.example.thirdparty:third-party-lib:1.0")
                }
                """.trimIndent()
            }

            testDependencyTransformations { reports ->
                reports.single {
                    it.sourceSetName == "commonTest" && it.scope == "api" && it.groupAndModule.startsWith("com.example.thirdparty")
                }.let {
                    assertEquals(setOf("commonMain"), it.allVisibleSourceSets)
                    assertEquals(setOf("commonMain"), it.newVisibleSourceSets)
                }

                reports.single {
                    it.sourceSetName == "jvmAndJsTest" && it.scope == "api" && it.groupAndModule.startsWith("com.example.thirdparty")
                }.let {
                    assertEquals(setOf("commonMain"), it.allVisibleSourceSets)
                    assertEquals(emptySet(), it.newVisibleSourceSets)
                }
            }
        }
    }

    @GradleTest
    @DisplayName("Dependencies in project should be correct with third-party library")
    fun testProjectDependencies(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(withGranularMetadata = false, gradleVersion = gradleVersion, localRepoDir = tempDir)

        with(
            nativeProject(
                "hierarchical-mpp-project-dependency",
                gradleVersion,
                localRepoDir = tempDir,
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
            )
        ) {
            build("publish", "assemble") {
                checkMyLibFoo(subprojectPrefix = "my-lib-foo", tempDir)
                checkMyLibBar(subprojectPrefix = "my-lib-bar", tempDir)
                checkMyApp(subprojectPrefix = "my-app")
            }
        }
    }

    @GradleTest
    @DisplayName("Check that only composite metadata artifacts are transformed")
    fun testOnlyCompositeMetadataArtifactsTransformed(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir)

        val regex = """artifact: '(.+)'""".toRegex()
        fun BuildResult.transformedArtifacts() = output
            .lineSequence()
            .filter { it.contains("Transform composite metadata") }
            .mapNotNull { regex.find(it)?.groups?.get(1)?.value }
            .map { File(it).name }
            .toSet()

        nativeProject(
            "my-lib-foo".withPrefix,
            gradleVersion,
            localRepoDir = tempDir,
            buildOptions = buildOptions
        ).run {
            build("publish") {
                assertEquals(
                    setOf(
                        "third-party-lib-metadata-1.0.jar",
                        "kotlin-stdlib-${buildOptions.kotlinVersion}-all.jar",
                    ),
                    transformedArtifacts()
                )
            }
        }

        nativeProject(
            "my-lib-bar".withPrefix,
            gradleVersion,
            localRepoDir = tempDir,
            buildOptions = buildOptions
        ).run {
            build("publish") {
                assertEquals(
                    setOf(
                        "my-lib-foo-metadata-1.0-all.jar",
                        "third-party-lib-metadata-1.0.jar",
                        "kotlin-stdlib-${buildOptions.kotlinVersion}-all.jar",
                    ),
                    transformedArtifacts()
                )
            }
        }

        nativeProject(
            "my-app".withPrefix,
            gradleVersion,
            localRepoDir = tempDir,
            buildOptions = buildOptions
        ).run {
            testDependencyTransformations {
                assertEquals(
                    setOf(
                        "my-lib-foo-metadata-1.0-all.jar",
                        "my-lib-bar-metadata-1.0-all.jar",
                        "third-party-lib-metadata-1.0.jar",
                        "kotlin-stdlib-${buildOptions.kotlinVersion}-all.jar",
                        "kotlin-dom-api-compat-${buildOptions.kotlinVersion}.klib"
                    ).toSortedSet(),
                    transformedArtifacts().toSortedSet()
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Works with published JS library")
    fun testHmppWithPublishedJsIrDependency(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        @Suppress("DEPRECATION")
        publishThirdPartyLib(
            projectName = "hierarchical-mpp-with-js-published-modules/third-party-lib",
            withGranularMetadata = true,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        )

        with(
            nativeProject(
                "hierarchical-mpp-with-js-published-modules/my-lib-foo",
                gradleVersion,
                localRepoDir = tempDir,
                buildOptions = defaultBuildOptions.copy(jsOptions = BuildOptions.JsOptions())
            )
        ) {
            build("publish", "assemble")
        }
    }

    @GradleTest
    @DisplayName("Works with project dependency on JS library")
    fun testHmppWithProjectJsIrDependency(gradleVersion: GradleVersion) {
        with(
            nativeProject(
                projectName = "hierarchical-mpp-with-js-project-dependency",
                gradleVersion = gradleVersion,
                buildOptions = defaultBuildOptions.copy(jsOptions = BuildOptions.JsOptions())
            )
        ) {
            build("assemble")
        }
    }

    @GradleTest
    @DisplayName("KT-48370: Multiplatform Gradle build fails for Native targets with \"we cannot choose between the following variants of project\"")
    fun testMultiModulesHmppKt48370(gradleVersion: GradleVersion) {
        project(
            "hierarchical-mpp-multi-modules",
            gradleVersion
        ) {
            build("assemble")
        }
    }

    @GradleTest
    @DisplayName("KT-57369 K2/MPP: supertypes established in actual-classifiers from other source sets are not visible")
    fun testHmppActualHasAdditionalSuperTypes(gradleVersion: GradleVersion) {
        project(
            "hierarchical-mpp-actual-has-additional-supertypes",
            gradleVersion
        ) {
            build("assemble")
        }
    }

    @GradleTest
    @DisplayName("Test that disambiguation attribute of Kotlin JVM Target is propagated to Java configurations")
    fun testMultipleJvmTargetsWithJavaAndDisambiguationAttributeKt31468(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-31468-multiple-jvm-targets-with-java",
            gradleVersion = gradleVersion
        ) {
            build("assemble", "testClasses") {
                assertTasksExecuted(
                    ":dependsOnPlainJvm:compileKotlinJvm",
                    ":dependsOnPlainJvm:compileJava",
                    ":dependsOnJvmWithJava:compileKotlinJvm",
                    ":dependsOnJvmWithJava:compileJava",

                    ":dependsOnPlainJvm:compileTestKotlinJvm",
                    ":dependsOnPlainJvm:compileTestJava",
                    ":dependsOnJvmWithJava:compileTestKotlinJvm",
                    ":dependsOnJvmWithJava:compileTestJava",
                )
            }
        }
    }

    @GradleTest
    @DisplayName("KT-54995: compileAppleMainKotlinMetadata fails on default parameters with `No value passed for parameter 'mustExist'")
    fun testCompileSharedNativeSourceSetWithOKIODependency(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-54995-compileSharedNative-with-okio",
            gradleVersion = gradleVersion
        ) {
            build("assemble") {
                assertFileExists(projectPath.resolve("build/libs/test-project-jvm.jar"))
                assertFileExists(projectPath.resolve("build/classes/kotlin/metadata/nativeMain/klib/test-project_nativeMain.klib"))
            }
        }
    }

    private fun publishThirdPartyLib(
        projectName: String = "third-party-lib".withPrefix,
        withGranularMetadata: Boolean,
        gradleVersion: GradleVersion,
        localRepoDir: Path,
        beforePublishing: TestProject.() -> Unit = { },
    ): TestProject =
        nativeProject(
            projectName = projectName,
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
            buildOptions = defaultBuildOptions.copy(jsOptions = BuildOptions.JsOptions())
        ).apply {
            beforePublishing()

            if (!withGranularMetadata) {
                val gradleProperties = projectPath.toFile().resolve("gradle.properties")
                gradleProperties.appendText("kotlin.internal.mpp.hierarchicalStructureByDefault=false${System.lineSeparator()}")
                gradleProperties.appendText("kotlin.internal.suppressGradlePluginErrors=PreHMPPFlagsError${System.lineSeparator()}")
            }
            build("publish")
        }

    private fun BuildResult.checkMyLibFoo(subprojectPrefix: String? = null, localRepoDir: Path) {
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            localRepoDir.toFile().resolve(
                "com/example/foo/my-lib-foo/1.0/my-lib-foo-1.0-all.jar"
            )
        ).use { publishedMetadataJar ->
            publishedMetadataJar.checkAllEntryNamesArePresent(
                "META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME",

                "commonMain/default/manifest",
                "commonMain/default/linkdata/package_com.example/",

                "jvmAndJsMain/default/manifest",
                "jvmAndJsMain/default/linkdata/package_com.example/",

                "linuxAndJsMain/default/manifest",
                "linuxAndJsMain/default/linkdata/package_com.example/"
            )

            val parsedProjectStructureMetadata: KotlinProjectStructureMetadata = publishedMetadataJar.getProjectStructureMetadata()

            val expectedProjectStructureMetadata = expectedProjectStructureMetadata(
                sourceSetModuleDependencies = mapOf(
                    "jvmAndJsMain" to setOf("com.example.thirdparty" to "third-party-lib"),
                    "linuxAndJsMain" to emptySet(),
                    "commonMain" to setOf("org.jetbrains.kotlin" to "kotlin-stdlib")
                )
            )

            assertEquals(
                expectedProjectStructureMetadata.sourceSetModuleDependencies.toSortedMap(),
                parsedProjectStructureMetadata.sourceSetModuleDependencies.toSortedMap()
            )

            assertEquals(expectedProjectStructureMetadata, parsedProjectStructureMetadata)
        }

        ZipFile(
            localRepoDir.toFile().resolve(
                "com/example/foo/my-lib-foo/1.0/my-lib-foo-1.0-sources.jar"
            )
        ).use { publishedSourcesJar ->
            publishedSourcesJar.checkExactEntries(
                "META-INF/MANIFEST.MF",
                "commonMain/Foo.kt",
                "jvmAndJsMain/FooJvmAndJs.kt",
                "linuxAndJsMain/FooLinuxAndJs.kt",
            )
        }
    }

    private fun BuildResult.checkMyLibBar(subprojectPrefix: String? = null, localRepoDir: Path) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()

        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            localRepoDir.toFile().resolve(
                "com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-all.jar"
            )
        ).use { publishedMetadataJar ->
            publishedMetadataJar.checkAllEntryNamesArePresent(
                "META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME",

                "commonMain/default/manifest",
                "commonMain/default/linkdata/package_com.example.bar/",

                "jvmAndJsMain/default/manifest",
                "jvmAndJsMain/default/linkdata/package_com.example.bar/",

                "linuxAndJsMain/default/manifest",
                "linuxAndJsMain/default/linkdata/package_com.example.bar/"
            )

            val parsedProjectStructureMetadata: KotlinProjectStructureMetadata = publishedMetadataJar.getProjectStructureMetadata()

            val expectedProjectStructureMetadata = expectedProjectStructureMetadata(
                sourceSetModuleDependencies = mapOf(
                    "jvmAndJsMain" to emptySet(),
                    "linuxAndJsMain" to emptySet(),
                    "commonMain" to setOf(
                        "org.jetbrains.kotlin" to "kotlin-stdlib",
                        "com.example.foo" to "my-lib-foo"
                    )
                )
            )

            assertEquals(
                expectedProjectStructureMetadata.sourceSetModuleDependencies.toSortedMap(),
                parsedProjectStructureMetadata.sourceSetModuleDependencies.toSortedMap()
            )

            assertEquals(expectedProjectStructureMetadata, parsedProjectStructureMetadata)
        }

        ZipFile(
            localRepoDir.toFile().resolve(
                "com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-sources.jar"
            )
        ).use { publishedSourcesJar ->
            publishedSourcesJar.checkExactEntries(
                "META-INF/MANIFEST.MF",
                "commonMain/Bar.kt",
                "jvmAndJsMain/BarJvmAndJs.kt",
                "linuxAndJsMain/BarLinuxAndJs.kt",
            )
        }

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-foo" to "main"
            ),
            shouldNotInclude = listOf(
                "my-lib-foo" to "jvmAndJsMain",
                "my-lib-foo" to "linuxAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileJvmAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-foo" to "main",
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            ),
            shouldNotInclude = listOf(
                "my-lib-foo" to "linuxAndJsMain"
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileLinuxAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-foo" to "linuxAndJsMain",
                "my-lib-foo" to "main"
            ),
            shouldNotInclude = listOf(
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )
    }

    private fun BuildResult.checkMyApp(subprojectPrefix: String? = null) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-bar" to "main",
                "my-lib-foo" to "main"
            ),
            shouldNotInclude = listOf(
                "my-lib-bar" to "jvmAndJsMain",
                "my-lib-bar" to "linuxAndJsMain",
                "my-lib-foo" to "jvmAndJsMain",
                "my-lib-foo" to "linuxAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileJvmAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-bar" to "main",
                "my-lib-bar" to "jvmAndJsMain",
                "my-lib-foo" to "main",
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            ),
            shouldNotInclude = listOf(
                "my-lib-bar" to "linuxAndJsMain",
                "my-lib-foo" to "linuxAndJsMain"
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileLinuxAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-bar" to "main",
                "my-lib-bar" to "linuxAndJsMain",
                "my-lib-foo" to "main",
                "my-lib-foo" to "linuxAndJsMain"
            ),
            shouldNotInclude = listOf(
                "my-lib-bar" to "jvmAndJsMain",
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )

        checkNamesOnCompileClasspath("$taskPrefix:compileLinuxAndJsMainKotlinMetadata")
    }

    private fun BuildResult.checkNamesOnCompileClasspath(
        taskPath: String,
        shouldInclude: Iterable<Pair<String, String>> = emptyList(),
        shouldNotInclude: Iterable<Pair<String, String>> = emptyList(),
    ) {
        val compilerArgsLine = output.lines().single { "$taskPath Kotlin compiler args:" in it }
        val classpathItems = compilerArgsLine.substringAfter("-classpath").substringBefore(" -").split(File.pathSeparator)

        val actualClasspath = classpathItems.joinToString("\n")

        shouldInclude.forEach { (module, sourceSet) ->
            assertTrue(
                "expected module '$module' source set '$sourceSet' on the classpath of task $taskPath. Actual classpath:\n$actualClasspath"
            ) {
                classpathItems.any { module in it && it.contains(sourceSet, ignoreCase = true) }
            }
        }

        shouldNotInclude.forEach { (module, sourceSet) ->
            assertTrue(
                "not expected module '$module' source set '$sourceSet' on the compile classpath of task $taskPath. " +
                        "Actual classpath:\n$actualClasspath"
            ) {
                classpathItems.none { module in it && it.contains(sourceSet, ignoreCase = true) }
            }
        }
    }

    private fun expectedTasks(subprojectPrefix: String?) = listOf(
        "generateProjectStructureMetadata",
        "transformCommonMainDependenciesMetadata",
        "transformJvmAndJsMainDependenciesMetadata",
        "transformLinuxAndJsMainDependenciesMetadata",
        "compileCommonMainKotlinMetadata",
        "compileJvmAndJsMainKotlinMetadata",
        "compileLinuxAndJsMainKotlinMetadata"
    ).map { task -> subprojectPrefix?.let { ":$it" }.orEmpty() + ":" + task }

    // the projects used in these tests are similar and only the dependencies differ:
    private fun expectedProjectStructureMetadata(
        sourceSetModuleDependencies: Map<String, Set<Pair<String, String>>>,
    ): KotlinProjectStructureMetadata {

        val jvmSourceSets = setOf("commonMain", "jvmAndJsMain")
        val jsSourceSets = setOf("commonMain", "jvmAndJsMain", "linuxAndJsMain")
        return KotlinProjectStructureMetadata(
            sourceSetNamesByVariantName = mapOf(
                "jsApiElements" to jsSourceSets,
                "jsRuntimeElements" to jsSourceSets,
                "jvmApiElements" to jvmSourceSets,
                "jvmRuntimeElements" to jvmSourceSets,
                "linuxX64ApiElements" to setOf("commonMain", "linuxAndJsMain")
            ),
            sourceSetsDependsOnRelation = mapOf(
                "jvmAndJsMain" to setOf("commonMain"),
                "linuxAndJsMain" to setOf("commonMain"),
                "commonMain" to emptySet()
            ),
            sourceSetModuleDependencies = sourceSetModuleDependencies.mapValues { (_, pairs) ->
                pairs.map {
                    ModuleDependencyIdentifier(it.first, it.second)
                }.toSet()
            },
            sourceSetCInteropMetadataDirectory = mapOf(),
            hostSpecificSourceSets = emptySet(),
            sourceSetBinaryLayout = sourceSetModuleDependencies.mapValues { SourceSetMetadataLayout.KLIB },
            sourceSetNames = setOf("commonMain", "jvmAndJsMain", "linuxAndJsMain"),
            isPublishedAsRoot = true
        )
    }

    private fun ZipFile.checkAllEntryNamesArePresent(vararg expectedEntryNames: String) {
        val entryNames = entries().asSequence().map { it.name }.toSet()
        val entryNamesString = entryNames.joinToString()
        expectedEntryNames.forEach {
            assertTrue("expecting entry $it in entry names $entryNamesString") { it in entryNames }
        }
    }

    private fun ZipFile.checkExactEntries(vararg expectedEntryNames: String, ignoreDirectories: Boolean = true) {
        val entryNamesSet = entries()
            .asSequence()
            .map { it.name }
            .run { if (ignoreDirectories) filterNot { it.endsWith("/") } else this }
            .sorted()
            .joinToString("\n")
        val expectedEntryNamesSet = expectedEntryNames.toList().sorted().joinToString("\n")
        assertEquals(expectedEntryNamesSet, entryNamesSet)
    }

    private fun ZipFile.sourceSetDirectories(): List<String> {
        return entries()
            .asSequence()
            .map { it.name.split("/").first() }
            .toSet()
            .minus("META-INF")
            .toList()
    }

    private fun ZipFile.getProjectStructureMetadata(): KotlinProjectStructureMetadata {
        val json = getInputStream(getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME")).reader().readText()
        return checkNotNull(parseKotlinSourceSetMetadataFromJson(json))
    }

    @GradleTest
    @DisplayName("Compile only dependency processing for metadata compilations")
    fun testCompileOnlyDependencyProcessingForMetadataCompilations(gradleVersion: GradleVersion, @TempDir tempDir: Path) =
        with(nativeProject("hierarchical-mpp-project-dependency", gradleVersion, localRepoDir = tempDir)) {
            publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir)

            subProject("my-lib-foo").buildGradleKts
                .appendText("\ndependencies { \"jvmAndJsMainCompileOnly\"(kotlin(\"test-annotations-common\")) }")
            projectPath.resolve("my-lib-foo/src/jvmAndJsMain/kotlin/UseCompileOnlyDependency.kt").writeText(
                """
            import kotlin.test.Test
                
            class UseCompileOnlyDependency {
                @Test
                fun myTest() = Unit
            }
            """.trimIndent()
            )

            build(":my-lib-foo:compileJvmAndJsMainKotlinMetadata")
        }

    @GradleTest
    @DisplayName("HMPP dependencies in js tests")
    fun testHmppDependenciesInJsTests(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        // For some reason Gradle 6.* fails with message about using deprecated API which will fail in 7.0
        // But for Gradle 7.* everything works, so seems false positive
        if (gradleVersion.baseVersion.version.substringBefore(".").toInt() < 7) {
            return
        }
        publishThirdPartyLib(
            withGranularMetadata = true,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        )
        with(project("hierarchical-mpp-js-test", gradleVersion)) {
            val taskToExecute = ":jsNodeTest"
            build(taskToExecute, "-PthirdPartyRepo=${tempDir.absolutePathString()}") {
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @GradleTest
    @DisplayName("Processing dependency declared in non root sourceSet")
    fun testProcessingDependencyDeclaredInNonRootSourceSet(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir)

        nativeProject(
            "my-lib-foo".withPrefix,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ).run {
            val intermediateMetadataCompileTask = ":compileJvmAndJsMainKotlinMetadata"

            build(intermediateMetadataCompileTask) {
                checkNamesOnCompileClasspath(
                    intermediateMetadataCompileTask,
                    shouldInclude = listOf(
                        "third-party-lib" to "commonMain"
                    )
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Check dependencies in non published sourceSets")
    fun testDependenciesInNonPublishedSourceSets(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir)

        nativeProject(
            "my-lib-foo".withPrefix,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ).run {
            testDependencyTransformations { reports ->
                reports.single {
                    it.sourceSetName == "jvmAndJsMain" && it.scope == "api" && it.groupAndModule.startsWith("com.example")
                }.let {
                    assertEquals(setOf("commonMain"), it.allVisibleSourceSets)
                    assertEquals(setOf("commonMain"), it.newVisibleSourceSets)
                }
            }
        }
    }

    @GradleTest
    @DisplayName("Check transitive dependency on self")
    fun testTransitiveDependencyOnSelf(gradleVersion: GradleVersion) =
        with(project("transitive-dep-on-self-hmpp", gradleVersion = gradleVersion)) {
            testDependencyTransformations(subproject = "lib") { reports ->
                reports.single {
                    it.sourceSetName == "commonTest" && it.scope == "implementation" && "libtests" in it.groupAndModule
                }.let {
                    assertEquals(setOf("commonMain", "jvmAndJsMain"), it.allVisibleSourceSets)
                }
            }
        }

    @GradleTest
    @OsCondition(enabledOnCI = [OS.LINUX, OS.MAC, OS.WINDOWS])
    @DisplayName("Test sources publication of a multiplatform library")
    fun testSourcesPublication(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        project(
            "mpp-sources-publication/producer",
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ).run {
            build("publish")

            fun macOnly(code: () -> List<String>): List<String> = if (OS.MAC.isCurrentOs) code() else emptyList()

            val rootModuleSources = listOf("test/lib/1.0/lib-1.0-sources.jar")
            val jvmModuleSources = listOf("test/lib-jvm/1.0/lib-jvm-1.0-sources.jar")
            val jvm2ModuleSources = listOf("test/lib-jvm2/1.0/lib-jvm2-1.0-sources.jar")
            val linuxX64ModuleSources = listOf("test/lib-linuxx64/1.0/lib-linuxx64-1.0-sources.jar")
            val linuxArm64ModuleSources = listOf("test/lib-linuxarm64/1.0/lib-linuxarm64-1.0-sources.jar")
            val iosX64ModuleSources = macOnly { listOf("test/lib-iosx64/1.0/lib-iosx64-1.0-sources.jar") }
            val iosArm64ModuleSources = macOnly { listOf("test/lib-iosarm64/1.0/lib-iosarm64-1.0-sources.jar") }
            val allPublishedSources = rootModuleSources +
                    jvmModuleSources + jvm2ModuleSources +
                    linuxX64ModuleSources + linuxArm64ModuleSources +
                    iosX64ModuleSources + iosArm64ModuleSources

            infix fun Pair<String, List<String>>.and(that: List<String>) = first to (second + that)

            // Here mentioned only source sets that should be published
            val expectedSourcePublicationLayout = listOf(
                "commonMain" to rootModuleSources
                        and jvmModuleSources and jvm2ModuleSources
                        and iosX64ModuleSources and iosArm64ModuleSources
                        and linuxArm64ModuleSources and linuxX64ModuleSources,
                "linuxMain" to rootModuleSources and linuxArm64ModuleSources and linuxX64ModuleSources,
                "jvmMain" to jvmModuleSources,
                "jvm2Main" to jvm2ModuleSources,
                // since commonJvmMain is compiled to JVM only, it doesn't appear it metadata variant,
                // it should be published only to jvm variants
                "commonJvmMain" to jvmModuleSources and jvm2ModuleSources,
                // iosMain is a host-specific sourceset and even though it isn't present in common metadata artifact
                // it should be published in common sources. more details: KT-54413
                "iosMain" to rootModuleSources and iosX64ModuleSources and iosArm64ModuleSources,
                "iosX64Main" to iosX64ModuleSources,
                "iosArm64Main" to iosArm64ModuleSources,
                "linuxX64Main" to linuxX64ModuleSources,
                "linuxArm64Main" to linuxArm64ModuleSources,
            )

            val expectedSourcePublicationLayoutBySourcesFile: Map<String, List<String>> = expectedSourcePublicationLayout
                .flatMap { (sourceSet, sources) -> sources.map { sourceSet to it } }
                .groupBy(
                    keySelector = { it.second },
                    valueTransform = { it.first }
                )

            val actualSourcePublicationLayoutBySourcesFile: Map<String, List<String>> = allPublishedSources
                .associateWith { jarPath ->
                    tempDir
                        .resolve(jarPath)
                        .toFile()
                        .let(::ZipFile)
                        .use { it.sourceSetDirectories() }
                }

            fun Map<String, List<String>>.stringifyForBeautifulDiff() = entries
                .sortedBy { it.key }
                .joinToString("\n") { "${it.key} => ${it.value.sorted()}" }

            assertEquals(
                expectedSourcePublicationLayoutBySourcesFile.stringifyForBeautifulDiff(),
                actualSourcePublicationLayoutBySourcesFile.stringifyForBeautifulDiff()
            )
        }

        project(
            "mpp-sources-publication/consumer",
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ) {
            buildGradleKts.appendText(
                """
                testResolutionToSourcesVariant(
                    "common",
                    KotlinPlatformType.common,
                    includeDisambiguation = false
                )

                testResolutionToSourcesVariant(
                    "jvm",
                    KotlinPlatformType.jvm
                )

                testResolutionToSourcesVariant(
                    "jvm2",
                    KotlinPlatformType.jvm
                )

                testResolutionToSourcesVariant(
                    "linuxX64",
                    KotlinPlatformType.native,
                    nativePlatform = "linux_x64"
                )
            """.trimIndent()
            )

            val expectedReports = mapOf(
                "common" to SourcesVariantResolutionReport(
                    files = listOf("lib-kotlin-1.0-sources.jar"),
                    dependencyToVariant = mapOf("test:lib:1.0" to "metadataSourcesElements")
                ),
                "jvm" to SourcesVariantResolutionReport(
                    files = listOf("lib-jvm-1.0-sources.jar"),
                    dependencyToVariant = mapOf(
                        "test:lib:1.0" to "jvmSourcesElements-published",
                        "test:lib-jvm:1.0" to "jvmSourcesElements-published"
                    )
                ),
                "jvm2" to SourcesVariantResolutionReport(
                    files = listOf("lib-jvm2-1.0-sources.jar"),
                    dependencyToVariant = mapOf(
                        "test:lib:1.0" to "jvm2SourcesElements-published",
                        "test:lib-jvm2:1.0" to "jvm2SourcesElements-published"
                    )
                ),
                "linuxX64" to SourcesVariantResolutionReport(
                    files = listOf("lib-linuxx64-1.0-sources.jar"),
                    dependencyToVariant = mapOf(
                        "test:lib:1.0" to "linuxX64SourcesElements-published",
                        "test:lib-linuxx64:1.0" to "linuxX64SourcesElements-published"
                    )
                ),
            )

            build("help") { // evaluate only
                val actualReports = SourcesVariantResolutionReport.parse(output, expectedReports.keys)
                assertEquals(expectedReports, actualReports)
            }
        }
    }

    @GradleTest
    @OsCondition(enabledOnCI = [OS.LINUX, OS.MAC, OS.WINDOWS])
    @DisplayName("Sources publication can be disabled per target")
    fun testDisableSourcesPublication(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        project(
            "mpp-sources-publication/producer",
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        ) {
            // Disable sources publication for all targets except JVM
            buildGradleKts.appendText(
                """
                    kotlin {
                        withSourcesJar(publish = false)
                    }
                    
                    kotlin.targets.getByName("jvm").withSourcesJar()
                """.trimIndent()
            )

            build("publish")

            val gradleModuleFileContent = tempDir.resolve("test/lib/1.0/lib-1.0.module").readText()
            fun assertNoSourcesPublished(expectedJarLocation: String, variantName: String) {
                val jarFile = tempDir.resolve(expectedJarLocation).toFile()
                if (jarFile.exists()) fail("Sources jar '$expectedJarLocation' shouldn't be published")
                if (gradleModuleFileContent.contains(variantName)) fail("Variant '$variantName' shouldn't be published")
            }

            assertNoSourcesPublished("test/lib/1.0/lib-1.0-sources.jar", "metadataSourcesElements")
            assertNoSourcesPublished("test/lib-linuxx64/1.0/lib-linuxx64-1.0-sources.jar", "linuxX64SourcesElements-published")
            assertNoSourcesPublished("test/lib-linuxarm64/1.0/lib-linuxarm64-1.0-sources.jar", "linuxArm64SourcesElements-published")
            if (OS.MAC.isCurrentOs) {
                assertNoSourcesPublished("test/lib-iosx64/1.0/lib-iosx64-1.0-sources.jar", "iosX64SourcesElements-published")
                assertNoSourcesPublished("test/lib-iosarm64/1.0/lib-iosarm64-1.0-sources.jar", "iosArm64SourcesElements-published")
            }

            // Check that JVM sources were published
            val jvmSourcesJar = tempDir.resolve("test/lib-jvm/1.0/lib-jvm-1.0-sources.jar")
            if (!jvmSourcesJar.exists()) {
                fail("JVM Sources should be published")
            }
            if (!gradleModuleFileContent.contains("jvmSourcesElements-published")) {
                fail("'jvmSourcesElements-published' variant should be published")
            }
        }
    }

    @GradleTest
    @DisplayName("KT-44845: all external dependencies is unresolved in IDE with kotlin.mpp.enableGranularSourceSetsMetadata=true")
    fun testMixedScopesFilesExistKt44845(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(withGranularMetadata = true, gradleVersion = gradleVersion, localRepoDir = tempDir)

        nativeProject(
            "my-lib-foo".withPrefix,
            gradleVersion,
            localRepoDir = tempDir
        ).run {
            buildGradleKts.appendText(
                """
                ${"\n"}
                dependencies {
                    "jvmAndJsMainImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                    "jvmAndJsMainCompileOnly"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                }
            """.trimIndent()
            )

            testDependencyTransformations { reports ->
                val reportsForJvmAndJsMain = reports.filter { it.sourceSetName == "jvmAndJsMain" }
                val thirdPartyLib = reportsForJvmAndJsMain.singleOrNull {
                    it.scope == "api" && it.groupAndModule.startsWith("com.example")
                }
                val coroutinesCore = reportsForJvmAndJsMain.singleOrNull {
                    it.scope == "implementation" && it.groupAndModule.contains("kotlinx-coroutines-core")
                }
                val serialization = reportsForJvmAndJsMain.singleOrNull {
                    it.scope == "compileOnly" && it.groupAndModule.contains("kotlinx-serialization-json")
                }
                assertNotNull(thirdPartyLib, "Expected report for third-party-lib")
                assertNotNull(coroutinesCore, "Expected report for kotlinx-coroutines-core")
                assertNotNull(serialization, "Expected report for kotlinx-serialization-json")

                listOf(thirdPartyLib, coroutinesCore, serialization).forEach { report ->
                    assertTrue(report.newVisibleSourceSets.isNotEmpty(), "Expected visible source sets for $report")
                    assertTrue(report.useFiles.isNotEmpty(), "Expected non-empty useFiles for $report")
                    report.useFiles.forEach { assertTrue(it.isFile, "Expected $it to exist for $report") }
                }
            }
        }
    }

    @GradleTest
    @DisplayName("KT-46417: [UNRESOLVED_REFERENCE] For project to project dependencies of native platform test source sets")
    fun testNativeLeafTestSourceSetsKt46417(gradleVersion: GradleVersion) {
        with(project("kt-46417-ios-test-source-sets", gradleVersion = gradleVersion)) {
            testDependencyTransformations("p2") { reports ->
                val report = reports.singleOrNull {
                    it.sourceSetName == "iosArm64Test" &&
                            it.scope == "implementation" &&
                            it.groupAndModule.endsWith(":p1")
                }
                assertNotNull(report, "No single report for 'iosArm64' and implementation scope")
                assertEquals(setOf("commonMain", "iosMain"), report.allVisibleSourceSets)
                assertTrue(report.groupAndModule.endsWith(":p1"))
            }
        }
    }

    @GradleTest
    @DisplayName("KT-52216: [TYPE_MISMATCH] Caused by unexpected metadata dependencies of leaf source sets")
    fun `test default platform compilation source set has no metadata dependencies`(gradleVersion: GradleVersion) {
        with(project("kt-52216", gradleVersion = gradleVersion)) {
            build(":lib:publish")
            testDependencyTransformations("p1") { reports ->
                for (leafSourceSetName in listOf("jvmMain", "jsMain", "linuxX64Main")) {
                    val report = reports.singleOrNull {
                        it.sourceSetName == leafSourceSetName && it.scope == "implementation" && it.groupAndModule == "kt52216:lib"
                    }
                    assertNotNull(report, "No transformation for $leafSourceSetName implementation")
                    assert(report.allVisibleSourceSets.isEmpty()) {
                        "All visible source sets for leaf platform source set should always be empty, but found: ${
                            report.allVisibleSourceSets.joinToString(prefix = "[", postfix = "]", separator = "; ")
                        }"
                    }
                    assert(report.newVisibleSourceSets.isEmpty()) {
                        "New visible source sets for leaf platform source set should always be empty, but found: ${
                            report.newVisibleSourceSets.joinToString(prefix = "[", postfix = "]", separator = "; ")
                        }"
                    }
                }
            }

            testDependencyTransformations("p2") { reports ->
                val commonReport = reports.singleOrNull {
                    it.sourceSetName == "commonMain" && it.scope == "implementation" && it.groupAndModule == "kt52216:lib"
                }
                assertNotNull(commonReport, "No transformation for commonMain implementation")
                assert(commonReport.allVisibleSourceSets.singleOrNull() == "commonMain") {
                    "All visible source sets of commonMain don't include library's commonMain"
                }
                assert(commonReport.newVisibleSourceSets.singleOrNull() == "commonMain") {
                    "New visible source sets of commonMain don't include library's commonMain"
                }

                for (targetName in listOf("jvm", "js", "linuxX64")) {
                    val intermediateReport = reports.singleOrNull {
                        it.sourceSetName == "${targetName}Intermediate" && it.scope == "implementation" && it.groupAndModule == "kt52216:lib"
                    }
                    val leafReport = reports.singleOrNull {
                        it.sourceSetName == "${targetName}Main" && it.scope == "implementation" && it.groupAndModule == "kt52216:lib"
                    }

                    assertNotNull(intermediateReport, "No transformation for ${targetName}Intermediate implementation")
                    assertNotNull(leafReport, "No transformation for ${targetName}Main implementation")

                    assert(intermediateReport.allVisibleSourceSets.singleOrNull() == "commonMain") {
                        "Intermediate transformation should contain commonMain in all visible source sets, but it doesn't for target: $targetName"
                    }
                    assert(leafReport.allVisibleSourceSets.isEmpty()) {
                        "All visible source sets for leaf platform source set should should be empty, but for target $targetName found: ${
                            leafReport.allVisibleSourceSets.joinToString(prefix = "[", postfix = "]", separator = "; ")
                        }"
                    }
                    assert(intermediateReport.newVisibleSourceSets.isEmpty()) {
                        "New visible source sets for intermediate source set should should be empty, but for target $targetName found: ${
                            leafReport.newVisibleSourceSets.joinToString(prefix = "[", postfix = "]", separator = "; ")
                        }"
                    }
                    assert(leafReport.newVisibleSourceSets.isEmpty()) {
                        "New visible source sets for leaf platform source set should always be empty, but for target $targetName found: ${
                            leafReport.newVisibleSourceSets.joinToString(prefix = "[", postfix = "]", separator = "; ")
                        }"
                    }
                }
            }
        }
    }

    @GradleTest
    @DisplayName("KT-55071: Shared Native Compilations: Use default parameters declared in dependsOn source set")
    fun `test shared native compilation with default parameters declared in dependsOn source set`(gradleVersion: GradleVersion) {
        with(project("kt-55071-compileSharedNative-withDefaultParameters", gradleVersion = gradleVersion)) {
            build(":producer:publish") {
                assertTasksExecuted(":producer:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":producer:compileSecondCommonMainKotlinMetadata")
                assertTasksExecuted(":producer:compileNativeMainKotlinMetadata")
            }

            build(":consumer:assemble") {
                assertTasksExecuted(":consumer:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":consumer:compileNativeMainKotlinMetadata")
            }
        }
    }

    @GradleTest
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_7_1)
    @DisplayName("KT-51940: Test ArtifactCollection.getResolvedArtifactsCompat with Gradle earlier than 7.4")
    fun `test getResolvedArtifactsCompat with Gradle earlier than 7_4`(gradleVersion: GradleVersion) {
        project("kt-51940-hmpp-resolves-configurations-during-configuration", gradleVersion = gradleVersion) {
            build("assemble", "--dry-run") {
                assertOutputContains("Configuration Resolved")
            }
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4)
    @DisplayName("KT-51940: Configurations should not resolved during configuration phase")
    fun `test configurations should not resolved during configuration phase`(gradleVersion: GradleVersion) {
        project("kt-51940-hmpp-resolves-configurations-during-configuration", gradleVersion = gradleVersion) {
            build("assemble", "--dry-run") {
                assertOutputDoesNotContain("Configuration Resolved")
            }
        }
    }

    @GradleTest
    @DisplayName("KT-57531: Kotlin Native Link with cycle in dependency constraints")
    fun `test Kotlin Native Link with cycle in dependency constraints`(gradleVersion: GradleVersion) {
        project("kt-57531-KotlinNativeLink-with-cycle-in-dependency-constraints", gradleVersion) {
            build("publish")
            build("assemble") {
                assertTasksExecuted(":consumer:linkDebugExecutableLinuxX64")
            }
        }
    }

    @GradleTest
    @DisplayName("It should be possible to disable default publications for stdlib and other kotlin libraries")
    fun `test disable default publications`(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        project("mppCustomPublicationLayout", gradleVersion = gradleVersion, localRepoDir = tempDir) {
            build(":libWithCustomLayout:publishKotlinPublicationToMavenRepository") {
                listOf("jvm.jar", "linuxArm64.klib", "linuxX64.klib")
                    .map { tempDir.resolve("test/libWithCustomLayout/1.0/libWithCustomLayout-1.0-$it") }
                    .forEach { if (!it.exists()) fail("Artifact $it does not exist") }
            }

            build(":libWithDefaultLayout:publish") {
                val pom = tempDir.resolve("test/libWithDefaultLayout-jvm/1.0/libWithDefaultLayout-jvm-1.0.pom").readText()
                val expectedDependency = """
                    |    <dependency>
                    |      <groupId>test</groupId>
                    |      <artifactId>libWithCustomLayout</artifactId>
                    |      <version>1.0</version>
                    |      <scope>compile</scope>
                    |    </dependency>
                """.trimMargin()

                fun String.asOneLine() = lines().joinToString(" ") { it.trim() }
                if (expectedDependency.asOneLine() !in pom.asOneLine()) {
                    fail("Expected to find:\n$expectedDependency\nin pom file:\n$pom")
                }
            }

            build(":app:assemble")
        }
    }

    @GradleTest
    @DisplayName("KT-56380: correct nullability inference in metadata compilations")
    fun `test correct nullability inference in metadata compilation`(gradleVersion: GradleVersion) {
        project("kt-56380_correct_nullability_inference", gradleVersion) {
            build(":b:compileCommonMainKotlinMetadata")
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    fun `test type safe project accessors with KotlinDependencyHandler`(gradleVersion: GradleVersion) {
        project("mpp-project-with-type-safe-accessors", gradleVersion) {
            build("help") {
                println(output)
                val actualDependencies = output.lineSequence()
                    .filter { it.startsWith("PROJECT_DEPENDENCY: ") }
                    .map { it.removePrefix("PROJECT_DEPENDENCY: ") }
                    .toList()

                assertEquals(
                    listOf(":foo", ":bar"),
                    actualDependencies
                )
            }
        }
    }


    private fun TestProject.testDependencyTransformations(
        subproject: String? = null,
        check: BuildResult.(reports: Iterable<DependencyTransformationReport>) -> Unit,
    ) {
        val buildGradleKts = (subproject?.let { subProject(subproject).buildGradleKts } ?: buildGradleKts).toFile()
        assert(buildGradleKts.exists()) { "Kotlin scripts are not found." }
        assert(buildGradleKts.extension == "kts") { "Only Kotlin scripts are supported." }

        val testTaskName = "reportDependencyTransformationsForTest"

        if (testTaskName !in buildGradleKts.readText()) {
            buildGradleKts.modify {
                "import ${DefaultKotlinSourceSet::class.qualifiedName}\n" + it + "\n" + """
                val $testTaskName by tasks.creating {
                    doFirst {
                        for (scope in listOf("api", "implementation", "compileOnly", "runtimeOnly")) {
                            println("========\n${'$'}scope\n")
                            
                            kotlin.sourceSets.withType<DefaultKotlinSourceSet>().forEach { sourceSet ->
                                println("--------\n${'$'}{sourceSet.name}")
                                
                                sourceSet
                                    .getDependenciesTransformation(
                                        "${'$'}{sourceSet.name}${'$'}{scope.capitalize()}DependenciesMetadata"
                                    ).forEach {
                                        val line = listOf(
                                                "${DependencyTransformationReport.TEST_OUTPUT_MARKER}",
                                                sourceSet.name,
                                                scope,
                                                it.groupId + ":" + it.moduleName,
                                                it.allVisibleSourceSets.joinToString(","),
                                                it.useFilesForSourceSets.keys.joinToString(","),
                                                it.useFilesForSourceSets.values.flatten().joinToString(",")
                                        )
                    
                                        println("        " + line.joinToString(" :: "))
                                    }
                                println()
                            }
                            println()
                        }
                    }
                }
                """.trimIndent()
            }
        }

        build(":${subproject?.plus(":").orEmpty()}$testTaskName") {
            val reports = output.lines()
                .filter { DependencyTransformationReport.TEST_OUTPUT_MARKER in it }
                .map { DependencyTransformationReport.parseTestOutputLine(it) }

            check(this, reports)
        }
    }

    internal data class DependencyTransformationReport(
        val sourceSetName: String,
        val scope: String,
        val groupAndModule: String,
        val allVisibleSourceSets: Set<String>,
        val newVisibleSourceSets: Set<String>, // those which the dependsOn parents don't see
        val useFiles: List<File>,
    ) {
        val isExcluded: Boolean get() = allVisibleSourceSets.isEmpty()

        companion object {
            const val TEST_OUTPUT_MARKER = "###transformation"
            const val TEST_OUTPUT_COMPONENT_SEPARATOR = " :: "
            const val TEST_OUTPUT_ITEMS_SEPARATOR = ","

            private operator fun <T> List<T>.component6() = this[5]

            fun parseTestOutputLine(line: String): DependencyTransformationReport {
                val tail = line.substringAfter(TEST_OUTPUT_MARKER + TEST_OUTPUT_COMPONENT_SEPARATOR)
                val (sourceSetName, scope, groupAndModule, allVisibleSourceSets, newVisibleSourceSets, useFiles) =
                    tail.split(TEST_OUTPUT_COMPONENT_SEPARATOR)
                return DependencyTransformationReport(
                    sourceSetName, scope, groupAndModule,
                    allVisibleSourceSets.split(TEST_OUTPUT_ITEMS_SEPARATOR).filter { it.isNotEmpty() }.toSet(),
                    newVisibleSourceSets.split(TEST_OUTPUT_ITEMS_SEPARATOR).filter { it.isNotEmpty() }.toSet(),
                    useFiles.split(TEST_OUTPUT_ITEMS_SEPARATOR).map { File(it) }
                )
            }
        }
    }

    private data class SourcesVariantResolutionReport(
        val files: List<String>,
        val dependencyToVariant: Map<String, String>,
    ) {
        companion object {
            fun parse(output: String, targetNames: Iterable<String>): Map<String, SourcesVariantResolutionReport> {
                val lines = output.lines()
                return targetNames.associateWith { targetName -> lines.parseForTarget(targetName) }
            }

            private fun List<String>.parseForTarget(targetName: String) = SourcesVariantResolutionReport(
                files = parseFiles(targetName),
                dependencyToVariant = parseResolvedDependencies(targetName)
            )

            private fun List<String>.betweenMarkers(
                start: String,
                end: String,
            ): List<String> {
                val startPos = indexOf(start)
                val endPos = indexOf(end)

                return subList(startPos + 1, endPos)
            }

            private fun List<String>.parseFiles(targetName: String): List<String> =
                betweenMarkers(
                    "<RESOLVED SOURCES FILE $targetName>",
                    "</RESOLVED SOURCES FILE $targetName>"
                )

            private fun List<String>.parseResolvedDependencies(targetName: String): Map<String, String> =
                betweenMarkers(
                    "<RESOLVED DEPENDENCIES OF $targetName>",
                    "</RESOLVED DEPENDENCIES OF $targetName>"
                ).associate { it.split(" => ").let { it[0] to it[1] } }
        }
    }
}
