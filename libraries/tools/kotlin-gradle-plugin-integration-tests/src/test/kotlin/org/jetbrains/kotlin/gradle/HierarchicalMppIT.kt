/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.internals.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import org.jetbrains.kotlin.gradle.internals.parseKotlinSourceSetMetadataFromJson
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

        Project("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("publish") {
                checkMyLibFoo(this, subprojectPrefix = null)
            }
        }

        Project("my-lib-bar", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("publish") {
                checkMyLibBar(this, subprojectPrefix = null)
            }
        }

        Project("my-app", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("assemble") {
                checkMyApp(this, subprojectPrefix = null)
            }
        }
    }

    @Test
    fun testNoSourceSetsVisibleIfNoVariantMatched() {
        publishThirdPartyLib(withGranularMetadata = true)

        Project("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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

        Project("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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

        with(Project("hierarchical-mpp-project-dependency", gradleVersion)) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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

        with(Project("my-lib-foo", gradleVersion, directoryPrefix)) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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
        with(Project("hierarchical-mpp-with-js-project-dependency", gradleVersion)) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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
        Project(projectName, gradleVersion, directoryPrefix).apply {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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
                "repo/com/example/foo/my-lib-foo-metadata/1.0/my-lib-foo-metadata-1.0-all.jar"
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
    }

    private fun checkMyLibBar(compiledProject: CompiledProject, subprojectPrefix: String?) = with(compiledProject) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()

        assertSuccessful()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            project.projectDir.parentFile.resolve(
                "repo/com/example/bar/my-lib-bar-metadata/1.0/my-lib-bar-metadata-1.0-all.jar"
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

        shouldInclude.forEach { (module, sourceSet) ->
            assertTrue("expected module '$module' source set '$sourceSet' on the classpath of task $taskPath") {
                classpathItems.any { module in it && it.contains(sourceSet, ignoreCase = true) }
            }
        }

        shouldNotInclude.forEach { (module, sourceSet) ->
            assertTrue("not expected module '$module' source set '$sourceSet' on the compile classpath of task $taskPath") {
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
            sourceSetBinaryLayout = sourceSetModuleDependencies.mapValues { SourceSetMetadataLayout.KLIB }
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
    fun testCompileOnlyDependencyProcessingForMetadataCompilations() = with(Project("hierarchical-mpp-project-dependency")) {
        publishThirdPartyLib(withGranularMetadata = true)
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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

        Project("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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

        Project("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

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
                                                it.useFilesForSourceSets.keys.joinToString(",")
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
        val newVisibleSourceSets: Set<String> // those which the dependsOn parents don't see
    ) {
        val isExcluded: Boolean get() = allVisibleSourceSets.isEmpty()

        companion object {
            const val TEST_OUTPUT_MARKER = "###transformation"
            const val TEST_OUTPUT_COMPONENT_SEPARATOR = " :: "
            const val TEST_OUTPUT_ITEMS_SEPARATOR = ","

            fun parseTestOutputLine(line: String): DependencyTransformationReport {
                val tail = line.substringAfter(TEST_OUTPUT_MARKER + TEST_OUTPUT_COMPONENT_SEPARATOR)
                val (sourceSetName, scope, groupAndModule, allVisibleSourceSets, newVisibleSourceSets) =
                    tail.split(TEST_OUTPUT_COMPONENT_SEPARATOR)
                return DependencyTransformationReport(
                    sourceSetName, scope, groupAndModule,
                    allVisibleSourceSets.split(TEST_OUTPUT_ITEMS_SEPARATOR).filter { it.isNotEmpty() }.toSet(),
                    newVisibleSourceSets.split(TEST_OUTPUT_ITEMS_SEPARATOR).filter { it.isNotEmpty() }.toSet()
                )
            }
        }
    }
}
