/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.embedProject
import org.jetbrains.kotlin.gradle.mpp.ResolvedVariantChecker.ResolvedVariantRequest
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.*
import org.junit.rules.ErrorCollector
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.*
import java.lang.Boolean as RefBoolean


// Tests are not working with AGP >= 7.1.0. See KT-57351 for details
class AndroidAndJavaConsumeMppLibBuiltByGradle7IT : AndroidAndJavaConsumeMppLibIT() {
    override val producerAgpVersion: AGPVersion = AGPVersion.v7_0_0
    override val producerGradleVersion: GradleVersionRequired = GradleVersionRequired.InRange(
        TestVersions.Gradle.G_7_0,
        TestVersions.Gradle.G_7_6
    )
}

@RunWith(Parameterized::class)
abstract class AndroidAndJavaConsumeMppLibIT : BaseGradleIT() {
    abstract val producerAgpVersion: AGPVersion
    abstract val producerGradleVersion: GradleVersionRequired

    @field:Rule
    @JvmField
    var collector: ErrorCollector = ErrorCollector()

    companion object {
        @field:ClassRule
        @JvmField
        val publishedLibraryCacheDir = TemporaryFolder()

        lateinit var publishedLibraryCache: MutableMap<Any, File>

        @JvmStatic
        @BeforeClass
        fun initCache() {
            publishedLibraryCache = mutableMapOf()
        }

        @JvmStatic
        @AfterClass
        fun deleteCache() {
            publishedLibraryCacheDir.delete()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "Consumer(AGP={3}, Gradle={4}), flavors={0}, debugOnly={1}, published={2}")
        fun testCases(): List<Array<Any>> {
            val consumers = listOf(
                AGPVersion.v4_2_0 to GradleVersionRequired.Exact(TestVersions.Gradle.G_6_9),
                AGPVersion.v7_0_0 to GradleVersionRequired.AtLeast(TestVersions.Gradle.G_7_6),
            )
            val buildParams = listOf(
                /* useFlavors, isAndroidPublishDebugOnly, isPublishedLibrary */
                arrayOf(false, false, false),
                arrayOf(false, true, false),
                arrayOf(true, false, false),
                arrayOf(true, true, false),
                arrayOf(false, false, true),
                arrayOf(false, true, true),
                arrayOf(true, false, true),
                arrayOf(true, true, true),
            )
            return consumers.flatMap { (agpVersion, gradleVersion) ->
                buildParams.map { arrayOf(*it, agpVersion, gradleVersion) }
            }
        }

        const val oldKotlinVersion = "1.5.31"
    }

    @Parameterized.Parameter(0)
    lateinit var useFlavorsParameter: RefBoolean

    @Parameterized.Parameter(1)
    lateinit var isAndroidPublishDebugOnlyParameter: RefBoolean

    @Parameterized.Parameter(2)
    lateinit var isPublishedLibraryParameter: RefBoolean

    @Parameterized.Parameter(3)
    lateinit var consumerAgpVersion: AGPVersion

    @Parameterized.Parameter(4)
    lateinit var consumerGradleVersion: GradleVersionRequired

    private val isAndroidPublishDebugOnly get() = isAndroidPublishDebugOnlyParameter.booleanValue()
    private val useFlavors get() = useFlavorsParameter.booleanValue()
    private val isPublishedLibrary get() = isPublishedLibraryParameter.booleanValue()

    private lateinit var dependencyProject: Project

    @Before
    override fun setUp() {
        super.setUp()

        val jdk11Home = File(System.getProperty("jdk11Home"))
        Assume.assumeTrue("This test requires JDK11 for AGP7", jdk11Home.isDirectory)

        val producerBuildOptions: BuildOptions

        dependencyProject = Project("new-mpp-android", producerGradleVersion, minLogLevel = LogLevel.INFO).apply {
            val usedProducerGradleVersion = chooseWrapperVersionOrFinishTest()
            producerBuildOptions = defaultBuildOptions().copy(
                javaHome = jdk11Home,
                androidHome = KtTestUtil.findAndroidSdk(),
                androidGradlePluginVersion = producerAgpVersion,
            ).suppressDeprecationWarningsOn(
                "AGP relies on FileTrees for ignoring empty directories when using @SkipWhenEmpty which has been deprecated (Gradle 7.4)"
            ) { options ->
                GradleVersion.version(usedProducerGradleVersion) >= GradleVersion.version(TestVersions.Gradle.G_7_4) && options.safeAndroidGradlePluginVersion < AGPVersion.v7_1_0
            }
            producerBuildOptions.androidHome?.let { acceptAndroidSdkLicenses(it) }
            projectDir.deleteRecursively()
            setupWorkingDir()
            // Don't need custom attributes here
            gradleBuildScript("lib").modify { text ->
                text.lines().filterNot { it.trimStart().startsWith("attribute(") }.joinToString("\n")
                    .let {
                        if (isAndroidPublishDebugOnly) it.checkedReplace(
                            "publishAllLibraryVariants()",
                            "publishLibraryVariants(\"${if (useFlavors) "flavor1Debug" else "debug"}\")"
                        ) else it
                    }
                    .let {
                        if (useFlavors) {
                            it + "\n" + """
                            android { 
                                flavorDimensions "myFlavor"
                                productFlavors {
                                    flavor1 { dimension "myFlavor" }
                                }
                            }
                            """.trimIndent()
                        } else it
                    }.let {
                        // Simulate the behavior with user-defined consumable configuration added with no proper attributes:
                        it + "\n" + """
                        configurations.create("legacyConfiguration") {
                            def bundlingAttribute = Attribute.of("org.gradle.dependency.bundling", String)
                            attributes.attribute(bundlingAttribute, "external")
                        }
                        """.trimIndent()
                    }
            }
        }

        if (isPublishedLibrary) {
            val cacheKey = listOf(
                useFlavors,
                isAndroidPublishDebugOnly,
                producerGradleVersion,
                producerAgpVersion
            )
            val cachedPublishedLibrary = publishedLibraryCache.computeIfAbsent(cacheKey) {
                val newTempDir = publishedLibraryCacheDir.newFolder()
                dependencyProject.build(":lib:publish", options = producerBuildOptions) {
                    assertSuccessful()
                }
                dependencyProject.projectDir.copyRecursively(newTempDir)
                newTempDir
            }
            cachedPublishedLibrary.copyRecursively(dependencyProject.projectDir, overwrite = true)
        }
    }

    @Test
    fun test() {
        runConsumerTest(dependencyProject, withKotlinVersion = null)
        runConsumerTest(dependencyProject, withKotlinVersion = defaultBuildOptions().kotlinVersion)

        // We don't want to test the behavior with old Kotlin project-to-project dependencies, only compatibility with publications:
        if (isPublishedLibrary) {
            runConsumerTest(dependencyProject, withKotlinVersion = oldKotlinVersion)
        }
    }

    /** Use [withKotlinVersion] = null for testing without Kotlin Gradle plugin */
    private fun runConsumerTest(
        dependencyProject: Project,
        withKotlinVersion: String?
    ) {
        if (producerGradleVersion != consumerGradleVersion && !isPublishedLibrary) {
            println("Testing project-to-project dependencies is only possible with one Gradle version on the consumer and producer sides")
            return
        }

        val repositoryLinesIfNeeded = if (isPublishedLibrary) """
                repositories {
                    maven { setUrl("${dependencyProject.projectDir.resolve("lib/build/repo").toURI()}") }                    
                }
            """.trimIndent() else ""

        val dependencyNotation =
            if (isPublishedLibrary)
                """"com.example:lib:1.0""""
            else "project(\":${dependencyProject.projectName}:lib\")"

        val usedConsumerGradleVersion: String
        val consumerProject = Project("AndroidProject", consumerGradleVersion, minLogLevel = LogLevel.INFO).apply {
            usedConsumerGradleVersion = chooseWrapperVersionOrFinishTest()
            projectDir.deleteRecursively()
            if (!isPublishedLibrary) {
                embedProject(dependencyProject)
                gradleSettingsScript().appendText(
                    "\ninclude(\":${dependencyProject.projectName}:lib\")"
                )
            }

            setupWorkingDir(applyLanguageVersion = withKotlinVersion != oldKotlinVersion)

            gradleBuildScript("Lib").apply {
                writeText(
                    // Remove the Kotlin plugin from the consumer project to check how pure-AGP Kotlin-less consumers resolve the dependency
                    readText().let {
                        if (withKotlinVersion != null) it else it.checkedReplace(
                            "id 'org.jetbrains.kotlin.android'",
                            "//"
                        )
                    }
                        .let { text ->
                            // If the test case doesn't assume flavors, remove the flavor setup lines:
                            if (useFlavors) text else text.lines().filter { !it.trim().startsWith("flavor") }.joinToString("\n")
                        } + "\n" + """
                        android {
                            buildTypes {
                                // We create a build type that is missing in the library in order to check how it resolves such an MPP lib
                                create("staging") { initWith(getByName("debug")) }
                            }
                        }
                        $repositoryLinesIfNeeded
                        dependencies {
                            implementation($dependencyNotation)
                        }
                    """.trimIndent()
                )
            }
        }

        val variantNamePublishedSuffix = if (isPublishedLibrary) "-published" else ""

        val variantForReleaseAndStaging = if (isAndroidPublishDebugOnly && isPublishedLibrary)
            "debugApiElements$variantNamePublishedSuffix"
        else "releaseApiElements$variantNamePublishedSuffix"

        fun nameWithFlavorIfNeeded(name: String) = if (useFlavors) "flavor1${
            name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
        }" else name

        val configurationToExpectedVariant = listOf(
            nameWithFlavorIfNeeded("debugCompileClasspath") to nameWithFlavorIfNeeded("debugApiElements$variantNamePublishedSuffix"),
            nameWithFlavorIfNeeded("releaseCompileClasspath") to nameWithFlavorIfNeeded(variantForReleaseAndStaging),
            nameWithFlavorIfNeeded("stagingCompileClasspath") to
                    if (isPublishedLibrary)
                        nameWithFlavorIfNeeded(variantForReleaseAndStaging)
                    // NB: unlike published library, both the release and debug variants provide the build type attribute
                    //     and therefore are not compatible with the "staging" consumer. So it can only use the JVM variant
                    else "jvmLibApiElements"
        )

        val dependencyInsightModuleName =
            if (isPublishedLibrary)
                "com.example:lib:1.0"
            else ":${dependencyProject.projectName}:lib"

        val consumerBuildOptions = defaultBuildOptions().copy(
            javaHome = File(System.getProperty("jdk11Home")),
            androidHome = KtTestUtil.findAndroidSdk(),
            androidGradlePluginVersion = consumerAgpVersion,
            kotlinVersion = withKotlinVersion ?: defaultBuildOptions().kotlinVersion
        ).suppressDeprecationWarningsOn(
            "AGP uses deprecated IncrementalTaskInputs (Gradle 7.5)"
        ) { options ->
            // looks a bit messy :/
            (!isPublishedLibrary && (withKotlinVersion != null || options.safeAndroidGradlePluginVersion >= AGPVersion.v7_0_0) || isPublishedLibrary && withKotlinVersion == oldKotlinVersion) &&
                    GradleVersion.version(usedConsumerGradleVersion) >= GradleVersion.version(TestVersions.Gradle.G_7_5) &&
                    options.safeAndroidGradlePluginVersion < AGPVersion.v7_3_0
        }

        val variantCheckRequests = mutableMapOf<ResolvedVariantRequest, String>()

        configurationToExpectedVariant.forEach { (configuration, expected) ->
            /* TODO: the issue KT-30961 is only fixed for AGP 7+. Older versions still reproduce the issue;
             *  This test asserts the existing incorrect behavior for older Gradle versions
             *  in the absence of the Kotlin Gradle plugin, in order to detect unintentional changes
             */
            val expectedVariant = if (consumerAgpVersion < AGPVersion.v7_0_0 && withKotlinVersion == null && !isPublishedLibrary) {
                "jvmLibApiElements"
            } else expected

            val resolvedVariantRequest = ResolvedVariantRequest("Lib", configuration, dependencyInsightModuleName)
            variantCheckRequests[resolvedVariantRequest] = expectedVariant
        }

        // Also test that a pure Java (Kotlin-less) project is able to resolve the MPP library dependency to the JVM variants:
        consumerProject.apply {
            gradleSettingsScript().appendText("\ninclude(\"pure-java\")")
            projectDir.resolve("pure-java/build.gradle.kts").also { it.parentFile.mkdirs() }.writeText(
                """
                plugins {
                    java
                    ${if (withKotlinVersion != null) "kotlin(\"jvm\")" else ""}
                }
                $repositoryLinesIfNeeded
                dependencies {
                    implementation($dependencyNotation)
                }
                """.trimIndent()
            )

            val resolvedVariantRequest = ResolvedVariantRequest("pure-java", "compileClasspath", dependencyInsightModuleName)
            variantCheckRequests[resolvedVariantRequest] = "jvmLibApiElements$variantNamePublishedSuffix"
        }

        try {
            ResolvedVariantChecker().assertResolvedSingleVariantsBatch(consumerProject, variantCheckRequests, consumerBuildOptions)
        } catch (e: AssertionError) {
            collector.addError(AssertionError("Failure with Kotlin version=${withKotlinVersion}", e))
        }
    }
}

class ResolvedVariantChecker {
    data class ResolvedVariantRequest(val subproject: String, val configuration: String, val dependencyFilter: String)

    data class ModuleVariantResult(val moduleName: String, val variantString: String)

    fun assertResolvedSingleVariantsBatch(
        project: BaseGradleIT.Project,
        assertions: Map<ResolvedVariantRequest, String>,
        buildOptions: BaseGradleIT.BuildOptions = project.testCase.defaultBuildOptions()
    ) {
        val actualResults = getResolvedVariantsBatch(project, assertions.keys, buildOptions)
        val mismatches = assertions.filterKeys {
            assertions.getValue(it) != actualResults.get(it)?.singleOrNull()?.variantString
        }
        if (mismatches.isNotEmpty()) {
            throw AssertionError(
                "Expected and actual variants do not match: \n" + mismatches.keys.joinToString("\n") {
                    val matches = actualResults.get(it)
                    val actualString = when {
                        matches == null || matches.isEmpty() -> "got no resolution result or resolution error!"
                        matches.size == 1 -> "got " + matches.singleOrNull()?.variantString
                        else -> "got multiple dependencies matched: $matches"
                    }
                    " * in configuration ${it.subproject}.${it.configuration}, " +
                            "dependency ${it.dependencyFilter} " +
                            "expected variant ${assertions.getValue(it)}, " +
                            actualString
                })
        }
    }

    fun getResolvedVariantsBatch(
        project: BaseGradleIT.Project,
        requests: Iterable<ResolvedVariantRequest>,
        buildOptions: BaseGradleIT.BuildOptions = project.testCase.defaultBuildOptions()
    ): Map<ResolvedVariantRequest, List<ModuleVariantResult>> = with(project.testCase) {
        with(project) {
            val requestsBySubproject = requests.groupBy { it.subproject }
            val tasksBySubproject = requestsBySubproject.entries.associate { (subproject, subprojectRequests) ->
                val buildScript = gradleBuildScript(subproject)
                val taskCodeByRequest = subprojectRequests.associateWith { request ->
                    when (buildScript.extension) {
                        "gradle" -> generateResolvedVariantTaskCodeGroovy(request.configuration, request.dependencyFilter)
                        "kts" -> generateResolvedVariantTaskCodeKts(request.configuration, request.dependencyFilter)
                        else -> error("unexpected build script $buildScript in project $projectName")
                    }
                }
                buildScript.appendText(taskCodeByRequest.values.joinToString("\n\n", "\n\n") { it.taskCode })
                subproject to taskCodeByRequest
            }

            val requestsByTaskName =
                tasksBySubproject.values.flatMap { it.entries }.associate { (request, task) -> task.taskName to request }

            val tasksToRun = tasksBySubproject.entries.flatMap { (subproject, tasks) -> tasks.map { ":$subproject:${it.value.taskName}" } }

            lateinit var result: Map<ResolvedVariantRequest, List<ModuleVariantResult>>
            build(*tasksToRun.toTypedArray(), options = buildOptions) {
                assertSuccessful()

                val variantReportRegex = Regex("$separator(.*?)$separator(.*?)$separator(.*)")
                val variantReports = variantReportRegex.findAll(output).map { match ->
                    val (task, module, variant) = match.destructured
                    val request = requestsByTaskName.getValue(task)
                    request to ModuleVariantResult(module, variant)
                }
                result = variantReports.groupBy(keySelector = { it.first }, valueTransform = { it.second })
            }
            result
        }
    }

    private data class TaskWithName(val taskCode: String, val taskName: String)

    private var taskId = 0
    private fun nextTaskId() = taskId++

    private val separator = "#ResolvedVariantChecker#"

    private fun generateResolvedVariantTaskCodeGroovy(configuration: String, dependencyNotation: String): TaskWithName {
        val taskName = "getResolvedVariants${nextTaskId()}"
        val taskCode = """
            tasks.register("$taskName") {
                doLast {
                    configurations.getByName("$configuration")
                       .incoming.resolutionResult
                       .allDependencies
                       .findAll { it.requested.displayName.contains("$dependencyNotation") }
                       .forEach { 
                           def variantString = 
                              (it instanceof org.gradle.api.artifacts.result.ResolvedDependencyResult) ? it.selected.variants.collect { it.displayName }.join(",") :
                              (it instanceof org.gradle.api.artifacts.result.UnresolvedDependencyResult) ? "error:" + it.failure.message?.replace("\n", "\\n") :
                              ""
                           println(
                               "$separator" + name + "$separator" + 
                               it.requested.displayName + "$separator" + 
                               variantString
                           ) 
                       }
                }
            }
        """.trimIndent()
        return TaskWithName(taskCode, taskName)
    }

    private fun generateResolvedVariantTaskCodeKts(configuration: String, dependencyNotation: String): TaskWithName {
        val taskName = "getResolvedVariants${nextTaskId()}"
        val taskCode = """
            tasks.register("$taskName") {
                doLast {
                    configurations.getByName("$configuration")
                       .incoming.resolutionResult
                       .allDependencies
                       .filter { "$dependencyNotation" in it.requested.displayName }
                       .forEach { 
                           val variantString = when (it) {
                              is org.gradle.api.artifacts.result.ResolvedDependencyResult -> it.selected.variants.map { it.displayName }.joinToString(",")
                              is org.gradle.api.artifacts.result.UnresolvedDependencyResult -> "error:" + it.failure.message?.replace("\n", "\\n")
                              else -> ""
                           }
                           println(
                              "$separator" + name + "$separator" + 
                              it.requested.displayName + "$separator" + 
                              variantString
                           ) 
                       }
                }
            }
        """.trimIndent()
        return TaskWithName(taskCode, taskName)
    }
}