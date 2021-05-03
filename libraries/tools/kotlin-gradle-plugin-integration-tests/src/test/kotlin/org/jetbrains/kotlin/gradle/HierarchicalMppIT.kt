/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.internals.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.internals.parseKotlinSourceSetMetadataFromJson
import org.jetbrains.kotlin.gradle.native.transformNativeTestProjectWithPluginDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.modify
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HierarchicalMppIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired
        get() = gradleVersion

    companion object {
        private val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT
    }

    @Test
    fun testPublishedModules() {
        publishThirdPartyLib(withGranularMetadata = false)

        transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            build("publish") {
                checkMyLibFoo(this, subprojectPrefix = null)
            }
        }

        transformNativeTestProjectWithPluginDsl("my-lib-bar", gradleVersion, "hierarchical-mpp-published-modules").run {
            build("publish") {
                checkMyLibBar(this, subprojectPrefix = null)
            }
        }

        transformNativeTestProjectWithPluginDsl("my-app", gradleVersion, "hierarchical-mpp-published-modules").run {
            build("assemble") {
                checkMyApp(this, subprojectPrefix = null)
            }
        }
    }

    @Test
    fun testNoSourceSetsVisibleIfNoVariantMatched() {
        publishThirdPartyLib(withGranularMetadata = true)

        transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            // --- Move the dependency from jvmAndJsMain to commonMain, where there's a linuxX64 target missing in the lib
            gradleBuildScript().modify {
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
                val jvmJsSourceSets = setOf("jvmMain", "jsMain", "jvmTest", "jsTest", "jvmAndJsMain", "jvmAndJsTest")
                thirdPartyLibApiVisibility.forEach {
                    if (it.sourceSetName in jvmJsSourceSets)
                        assertTrue("$it") { it.allVisibleSourceSets == setOf("commonMain") }
                }
            }
        }
    }

    @Test
    fun testDependenciesInTests() {
        publishThirdPartyLib(withGranularMetadata = true) {
            projectDir.resolve("src/jvmMain").copyRecursively(projectDir.resolve("src/linuxX64Main"))
            gradleBuildScript().appendText("\nkotlin.linuxX64()")
        }

        transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
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
                    assertSuccessful()
                    existingFilesFromReports.forEach { assertTrue("Expected that $it exists after clean build.") { it.isFile } }
                }
            }

            // --- Move the dependency from jvmAndJsMain to commonMain, expect that it is now propagated to commonTest:
            gradleBuildScript().modify {
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
            gradleBuildScript().modify {
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

    @Test
    fun testProjectDependencies() {
        publishThirdPartyLib(withGranularMetadata = false)

        with(transformNativeTestProjectWithPluginDsl("hierarchical-mpp-project-dependency", gradleVersion)) {
            build("publish", "assemble") {
                checkMyLibFoo(this, subprojectPrefix = "my-lib-foo")
                checkMyLibBar(this, subprojectPrefix = "my-lib-bar")
                checkMyApp(this, subprojectPrefix = "my-app")
            }
        }
    }

    @Test
    fun testHmppWithPublishedJsBothDependency() {
        val directoryPrefix = "hierarchical-mpp-with-js-published-modules"
        publishThirdPartyLib(
            projectName = "third-party-lib",
            directoryPrefix = directoryPrefix,
            withGranularMetadata = true,
            jsCompilerType = KotlinJsCompilerType.BOTH
        )

        with(transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, directoryPrefix)) {
            build(
                "publish",
                "assemble",
                options = defaultBuildOptions().copy(jsCompilerType = KotlinJsCompilerType.IR)
            ) {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testHmppWithProjectJsIrDependency() {
        with(transformNativeTestProjectWithPluginDsl("hierarchical-mpp-with-js-project-dependency", gradleVersion)) {
            build(
                "assemble",
                options = defaultBuildOptions().copy(jsCompilerType = KotlinJsCompilerType.IR)
            ) {
                assertSuccessful()
            }
        }
    }

    private fun publishThirdPartyLib(
        projectName: String = "third-party-lib",
        directoryPrefix: String = "hierarchical-mpp-published-modules",
        withGranularMetadata: Boolean,
        jsCompilerType: KotlinJsCompilerType = KotlinJsCompilerType.LEGACY,
        beforePublishing: Project.() -> Unit = { }
    ): Project =
        transformNativeTestProjectWithPluginDsl(projectName, gradleVersion, directoryPrefix).apply {
            beforePublishing()

            if (withGranularMetadata) {
                projectDir.resolve("gradle.properties").appendText("kotlin.mpp.enableGranularSourceSetsMetadata=true")
            }

            build(
                "publish",
                options = defaultBuildOptions().copy(jsCompilerType = jsCompilerType)
            ) {
                assertSuccessful()
            }
        }

    private fun checkMyLibFoo(compiledProject: CompiledProject, subprojectPrefix: String? = null) = with(compiledProject) {
        assertSuccessful()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            project.projectDir.parentFile.resolve(
                "repo/com/example/foo/my-lib-foo/1.0/my-lib-foo-1.0-all.jar"
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
            project.projectDir.parentFile.resolve(
                "repo/com/example/foo/my-lib-foo/1.0/my-lib-foo-1.0-sources.jar"
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

    private fun checkMyLibBar(compiledProject: CompiledProject, subprojectPrefix: String?) = with(compiledProject) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()

        assertSuccessful()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            project.projectDir.parentFile.resolve(
                "repo/com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-all.jar"
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
            project.projectDir.parentFile.resolve(
                "repo/com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-sources.jar"
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

    private fun checkMyApp(compiledProject: CompiledProject, subprojectPrefix: String?) = with(compiledProject) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()

        assertSuccessful()
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

    private fun CompiledProject.checkNamesOnCompileClasspath(
        taskPath: String,
        shouldInclude: Iterable<Pair<String, String>> = emptyList(),
        shouldNotInclude: Iterable<Pair<String, String>> = emptyList()
    ) {
        val taskOutput = getOutputForTask(taskPath.removePrefix(":"))
        val compilerArgsLine = taskOutput.lines().single { "Kotlin compiler args:" in it }
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

    @Test
    fun testCompileOnlyDependencyProcessingForMetadataCompilations() =
        with(transformNativeTestProjectWithPluginDsl("hierarchical-mpp-project-dependency")) {
            publishThirdPartyLib(withGranularMetadata = true)

            gradleBuildScript("my-lib-foo").appendText("\ndependencies { \"jvmAndJsMainCompileOnly\"(kotlin(\"test-annotations-common\")) }")
            projectDir.resolve("my-lib-foo/src/jvmAndJsMain/kotlin/UseCompileOnlyDependency.kt").writeText(
                """
            import kotlin.test.Test
                
            class UseCompileOnlyDependency {
                @Test
                fun myTest() = Unit
            }
            """.trimIndent()
            )

            build(":my-lib-foo:compileJvmAndJsMainKotlinMetadata") {
                assertSuccessful()
            }
        }

    @Test
    fun testHmppDependenciesInJsTests() {
        val thirdPartyRepo = publishThirdPartyLib(withGranularMetadata = true).projectDir.parentFile.resolve("repo")
        with(Project("hierarchical-mpp-js-test")) {
            val taskToExecute = ":jsNodeTest"
            build(taskToExecute, "-PthirdPartyRepo=$thirdPartyRepo") {
                assertSuccessful()
                assertTasksExecuted(taskToExecute)
            }
        }
    }

    @Test
    fun testProcessingDependencyDeclaredInNonRootSourceSet() {
        publishThirdPartyLib(withGranularMetadata = true)

        transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            val intermediateMetadataCompileTask = ":compileJvmAndJsMainKotlinMetadata"

            build(intermediateMetadataCompileTask) {
                assertSuccessful()

                checkNamesOnCompileClasspath(
                    intermediateMetadataCompileTask,
                    shouldInclude = listOf(
                        "third-party-lib" to "commonMain"
                    )
                )
            }
        }
    }

    @Test
    fun testDependenciesInNonPublishedSourceSets() {
        publishThirdPartyLib(withGranularMetadata = true)

        transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
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

    @Test
    fun testTransitiveDependencyOnSelf() = with(Project("transitive-dep-on-self-hmpp")) {
        testDependencyTransformations(subproject = "lib") { reports ->
            reports.single {
                it.sourceSetName == "commonTest" && it.scope == "implementation" && "libtests" in it.groupAndModule
            }.let {
                assertEquals(setOf("commonMain", "jvmAndJsMain"), it.allVisibleSourceSets)
            }
        }
    }

    @Test
    fun testMixedScopesFilesExistKt44845() {
        publishThirdPartyLib(withGranularMetadata = true)

        transformNativeTestProjectWithPluginDsl("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            gradleBuildScript().appendText(
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
                val thirdPartyLib = reportsForJvmAndJsMain.single {
                    it.scope == "api" && it.groupAndModule.startsWith("com.example")
                }
                val coroutinesCore = reportsForJvmAndJsMain.single {
                    it.scope == "implementation" && it.groupAndModule.contains("kotlinx-coroutines-core")
                }
                val serialization = reportsForJvmAndJsMain.single {
                    it.scope == "compileOnly" && it.groupAndModule.contains("kotlinx-serialization-json")
                }
                listOf(thirdPartyLib, coroutinesCore, serialization).forEach { report ->
                    assertTrue(report.newVisibleSourceSets.isNotEmpty(), "Expected visible source sets for $report")
                    assertTrue(report.useFiles.isNotEmpty(), "Expected non-empty useFiles for $report")
                    report.useFiles.forEach { assertTrue(it.isFile, "Expected $it to exist for $report") }
                }
            }
        }
    }

    @Test
    fun testNativeLeafTestSourceSetsKt46417() {
        with(Project("kt-46417-ios-test-source-sets")) {
            testDependencyTransformations("p2") { reports ->
                val report = reports.singleOrNull { it.sourceSetName == "iosArm64Test" && it.scope == "implementation" }
                assertNotNull(report, "No single report for 'iosArm64' and implementation scope")
                assertEquals(setOf("commonMain", "iosMain"), report.allVisibleSourceSets)
                assertTrue(report.groupAndModule.endsWith(":p1"))
            }
        }
    }

    private fun Project.testDependencyTransformations(
        subproject: String? = null,
        check: CompiledProject.(reports: Iterable<DependencyTransformationReport>) -> Unit
    ) {
        setupWorkingDir()
        val buildGradleKts = gradleBuildScript(subproject)
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
            assertSuccessful()

            val reports = output.lines()
                .filter { DependencyTransformationReport.TEST_OUTPUT_MARKER in it }
                .map { DependencyTransformationReport.parseTestOutputLine(it) }

            check(this, reports)
        }
    }

    private data class DependencyTransformationReport(
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
