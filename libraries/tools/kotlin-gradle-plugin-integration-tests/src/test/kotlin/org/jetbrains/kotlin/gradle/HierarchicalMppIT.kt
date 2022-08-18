/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.internals.parseKotlinSourceSetMetadataFromJson
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@MppGradlePluginTests
@DisplayName("Hierarchical multiplatform")
class HierarchicalMppIT : KGPBaseTest() {

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
            buildGradleKts.appendText("kotlin.linuxX64()")
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
    @DisplayName("Works with published JS library")
    fun testHmppWithPublishedJsBothDependency(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishThirdPartyLib(
            projectName = "hierarchical-mpp-with-js-published-modules/third-party-lib",
            withGranularMetadata = true,
            jsCompilerType = KotlinJsCompilerType.BOTH,
            gradleVersion = gradleVersion,
            localRepoDir = tempDir
        )

        with(
            nativeProject(
                "hierarchical-mpp-with-js-published-modules/my-lib-foo",
                gradleVersion,
                localRepoDir = tempDir,
                buildOptions = defaultBuildOptions.copy(jsOptions = BuildOptions.JsOptions(jsCompilerType = KotlinJsCompilerType.IR))
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
                buildOptions = defaultBuildOptions.copy(jsOptions = BuildOptions.JsOptions(jsCompilerType = KotlinJsCompilerType.IR))
            )
        ) {
            build("assemble")
        }
    }

    @GradleTest
    @DisplayName("KT-48370: Multiplatform Gradle build fails for Native targets with \"we cannot choose between the following variants of project\"")
    fun testMultiModulesHmppKt48370(gradleVersion: GradleVersion) = with(
        project(
            projectName = "hierarchical-mpp-multi-modules",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                warningMode = WarningMode.Summary
            )
        )
    ) {
        build(
            "assemble",
        )
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

    private fun publishThirdPartyLib(
        projectName: String = "third-party-lib".withPrefix,
        withGranularMetadata: Boolean,
        jsCompilerType: KotlinJsCompilerType = KotlinJsCompilerType.LEGACY,
        gradleVersion: GradleVersion,
        localRepoDir: Path,
        beforePublishing: TestProject.() -> Unit = { }
    ): TestProject =
        nativeProject(
            projectName = projectName,
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
            buildOptions = defaultBuildOptions.copy(jsOptions = BuildOptions.JsOptions(jsCompilerType = jsCompilerType))
        ).apply {
            beforePublishing()

            if (!withGranularMetadata) {
                projectPath.toFile().resolve("gradle.properties").appendText("kotlin.internal.mpp.hierarchicalStructureByDefault=false")
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
                    "commonMain" to emptySet()
                )
            )

            assertEquals(expectedProjectStructureMetadata, parsedProjectStructureMetadata)
        }

        ZipFile(
            localRepoDir.toFile().resolve(
                "com/example/foo/my-lib-foo/1.0/my-lib-foo-1.0-sources.jar"
            )
        ).use { publishedSourcesJar ->
            publishedSourcesJar.checkAllEntryNamesArePresent(
                "commonMain/Foo.kt",
                "jvmAndJsMain/FooJvmAndJs.kt",
                "linuxAndJsMain/FooLinuxAndJs.kt",
                "linuxX64Main/FooLinux.kt"
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
                    "jvmAndJsMain" to setOf(),
                    "linuxAndJsMain" to emptySet(),
                    "commonMain" to setOf("com.example.foo" to "my-lib-foo")
                )
            )

            assertEquals(expectedProjectStructureMetadata, parsedProjectStructureMetadata)
        }

        ZipFile(
            localRepoDir.toFile().resolve(
                "com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-sources.jar"
            )
        ).use { publishedSourcesJar ->
            publishedSourcesJar.checkAllEntryNamesArePresent(
                "commonMain/Bar.kt",
                "jvmAndJsMain/BarJvmAndJs.kt",
                "linuxAndJsMain/BarLinuxAndJs.kt",
                "linuxX64Main/BarLinux.kt"
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
        shouldNotInclude: Iterable<Pair<String, String>> = emptyList()
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
        "compileKotlinMetadata",
        "compileJvmAndJsMainKotlinMetadata",
        "compileLinuxAndJsMainKotlinMetadata"
    ).map { task -> subprojectPrefix?.let { ":$it" }.orEmpty() + ":" + task }

    // the projects used in these tests are similar and only the dependencies differ:
    private fun expectedProjectStructureMetadata(
        sourceSetModuleDependencies: Map<String, Set<Pair<String, String>>>
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
                    "jvmAndJsMainImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
                    "jvmAndJsMainCompileOnly"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
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
                val report = reports.singleOrNull { it.sourceSetName == "iosArm64Test" && it.scope == "implementation" }
                assertNotNull(report, "No single report for 'iosArm64' and implementation scope")
                assertEquals(setOf("commonMain", "iosMain"), report.allVisibleSourceSets)
                assertTrue(report.groupAndModule.endsWith(":p1"))
            }
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_4, maxVersion = TestVersions.Gradle.G_7_4)
    @DisplayName("KT-51946: Temporarily mark HMPP tasks as notCompatibleWithConfigurationCache for Gradle 7.4")
    fun testHmppTasksAreNotIncludedInGradleConfigurationCache(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        with(project("hmppGradleConfigurationCache", gradleVersion = gradleVersion, localRepoDir = tempDir)) {
            val options = buildOptions.copy(configurationCache = true, configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL)

            build(":lib:publish") {
                assertTasksExecuted(":lib:publish")
            }

            val configCacheIncompatibleTasks = listOf(
                ":lib:transformCommonMainDependenciesMetadata",
            )

            build("clean", "assemble", buildOptions = options) {
                assertTasksExecuted(configCacheIncompatibleTasks)
                configCacheIncompatibleTasks.forEach { task ->
                    assertOutputContains(
                        """Task `${task}` of type `.+`: .+(at execution time is unsupported|not supported with the configuration cache)"""
                            .toRegex()
                    )
                }
            }

            build("clean", "assemble", buildOptions = options) {
                assertOutputContains("Configuration cache entry discarded")
                assertTasksExecuted(configCacheIncompatibleTasks)
            }
        }
    }
    @GradleTest
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_7_3)
    @DisplayName("KT-51946: Print warning on tasks that are not compatible with configuration cache")
    fun testHmppTasksReportConfigurationCacheWarningForGradleLessThan74(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        with(project("hmppGradleConfigurationCache", gradleVersion = gradleVersion, localRepoDir = tempDir)) {
            build(":lib:publish")

            // Assert that no warnings are shown when configuration-cache is not enabled
            build("clean", "assemble") {
                assertOutputDoesNotContain("""Task \S+ is not compatible with configuration cache""".toRegex())
            }

            val options = buildOptions.copy(configurationCache = true, configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL)
            buildAndFail("clean", "assemble", buildOptions = options) {
                assertOutputContains("""Task \S+ is not compatible with configuration cache""".toRegex())
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

    private fun TestProject.testDependencyTransformations(
        subproject: String? = null,
        check: BuildResult.(reports: Iterable<DependencyTransformationReport>) -> Unit
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
        val useFiles: List<File>
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
}
