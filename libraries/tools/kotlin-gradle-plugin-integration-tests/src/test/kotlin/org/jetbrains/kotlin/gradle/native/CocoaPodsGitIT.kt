/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native


import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_GEN_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_IMPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_INSTALL_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SETUP_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SPEC_TASK_NAME
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Git connected K/N tests with cocoapods")
@NativeGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@OptIn(EnvironmentalVariablesOverride::class)
class CocoaPodsGitIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
            cocoapodsGenerateWrapper = true
        )
    )

    private val templateProjectName = "native-cocoapods-template"
    private val groovyTemplateProjectName = "native-cocoapods-template-groovy"

    private val defaultPodRepo = "https://github.com/AFNetworking/AFNetworking"
    private val defaultPodName = "AFNetworking"
    private val defaultTarget = "IOS"
    private val defaultFamily = "ios"
    private val defaultSDK = "iphonesimulator"
    private val cinteropTaskName = ":cinterop"
    private val defaultCinteropTaskName = cinteropTaskName + defaultPodName + defaultTarget

    private val podImportTaskName = ":$POD_IMPORT_TASK_NAME"
    private val podspecTaskName = ":$POD_SPEC_TASK_NAME"
    private val podGenTaskName = ":$POD_GEN_TASK_NAME"
    private val podSetupBuildTaskName = ":$POD_SETUP_BUILD_TASK_NAME"
    private val podBuildTaskName = ":$POD_BUILD_TASK_NAME"
    private val podInstallTaskName = ":$POD_INSTALL_TASK_NAME"

    private val defaultPodInstallSyntheticTaskName = ":podInstallSyntheticIos"
    private val defaultPodGenTaskName = podGenFullTaskName()
    private val defaultBuildTaskName = podBuildFullTaskName()
    private val defaultSetupBuildTaskName = podSetupBuildFullTaskName()

    private fun podGenFullTaskName(familyName: String = defaultFamily) =
        podGenTaskName + familyName.capitalize()

    private fun podSetupBuildFullTaskName(podName: String = defaultPodName, sdkName: String = defaultSDK) =
        podSetupBuildTaskName + podName.capitalize() + sdkName.capitalize()

    private fun podBuildFullTaskName(podName: String = defaultPodName, sdkName: String = defaultSDK) =
        podBuildTaskName + podName.capitalize() + sdkName.capitalize()

    private fun cinteropFullTaskName(podName: String = defaultPodName, targetName: String = defaultTarget) =
        cinteropTaskName + podName.capitalize() + targetName.capitalize()

    @BeforeAll
    fun setUp() {
        ensureCocoapodsInstalled()
    }

    @DisplayName("Downloading pod from git without specifying neither tag not commit")
    @GradleTest
    fun testPodDownloadGitNoTagNorCommit(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion)
    }

    @DisplayName("Downloading pod from git with specifying tag")
    @GradleTest
    fun testPodDownloadGitTag(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion, tag = "4.0.0")
    }

    @DisplayName("Downloading pod from git with specifying commit")
    @GradleTest
    fun testPodDownloadGitCommit(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion, commit = "9c07ac0a5645abb58850253eeb109ed0dca515c1")
    }

    @DisplayName("Downloading pod from git with specifying branch")
    @GradleTest
    fun testPodDownloadGitBranch(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion, branch = "2974")
    }

    @DisplayName("Downloading pod's subspec from git")
    @GradleTest
    fun testPodDownloadGitSubspec(gradleVersion: GradleVersion) {
        doTestGit(
            gradleVersion,
            repo = "https://github.com/SDWebImage/SDWebImage.git",
            pod = "SDWebImage/MapKit",
            tag = "5.9.2"
        )
    }

    @DisplayName("Downloading pod from git with specifying branch and commit")
    @GradleTest
    fun testPodDownloadGitBranchAndCommit(gradleVersion: GradleVersion) {
        doTestGit(
            gradleVersion,
            branch = "2974",
            commit = "21637dd6164c0641e414bdaf3885af6f1ef15aee"
        )
    }


    @DisplayName("Downloading pod from git (tag priority is bigger than branch priority)")
    @GradleTest
    fun testPodDownloadGitBranchAndTag(gradleVersion: GradleVersion) {
        doTestGit(
            gradleVersion,
            tag = "4.0.0",
            branch = "2974"
        )
    }

    @DisplayName("Downloading pod from git with specifying tag for groovy build file")
    @GradleTest
    fun testGroovyDownloadAndImport(gradleVersion: GradleVersion) {
        doTestGit(
            gradleVersion,
            groovyTemplateProjectName,
            tag = "4.0.0",
            isGradleBuildScript = true
        )
    }

    @DisplayName("Checks that task cinterop is up to date during the second build")
    @GradleTest
    fun testCinteropUpToDate(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion) {

            build(
                "syncFramework",
                buildOptions = defaultBuildOptions.copy(
                    nativeOptions = defaultBuildOptions.nativeOptions.copy(
                        cocoapodsGenerateWrapper = true,
                        cocoapodsArchs = "x86_64",
                        cocoapodsConfiguration = "Debug",
                        cocoapodsPlatform = "iphonesimulator",
                    )
                )
            ) {
                assertTasksUpToDate(
                    defaultCinteropTaskName
                )
            }
        }

    }

    @DisplayName("UTD test")
    @GradleTest
    fun basicUTDTest(gradleVersion: GradleVersion) {
        val tasks = listOf(
            podspecTaskName,
            defaultPodGenTaskName,
            defaultPodInstallSyntheticTaskName,
            defaultSetupBuildTaskName,
            defaultBuildTaskName,
            defaultCinteropTaskName,
        )
        doTestGit(
            gradleVersion,
            testImportAssertions = { assertTasksExecuted(tasks) }
        ) {
            testImport {
                assertTasksUpToDate(tasks)
            }
        }
    }

    @DisplayName("UTD with adding and removing pod")
    @GradleTest
    fun testUTDPodAdded(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(defaultPodName, produceGitBlock())
            testImport()

            val anotherPodName = "Alamofire"
            val anotherPodRepo = "https://github.com/Alamofire/Alamofire"
            buildGradleKts.addPod(anotherPodName, produceGitBlock(anotherPodRepo))
            testImport(listOf(defaultPodRepo, anotherPodRepo)) {

                assertTasksExecuted(
                    podspecTaskName,
                    defaultPodGenTaskName,
                    podSetupBuildFullTaskName(anotherPodName),
                    podBuildFullTaskName(anotherPodName),
                    cinteropFullTaskName(anotherPodName)
                )
                assertTasksUpToDate(
                    defaultSetupBuildTaskName,
                    defaultBuildTaskName,
                    defaultCinteropTaskName
                )

                buildGradleKts.removePod(anotherPodName)

                testImport {
                    assertOutputDoesNotContain(podBuildFullTaskName(anotherPodName))
                    assertOutputDoesNotContain(cinteropFullTaskName(anotherPodName))
                    assertTasksUpToDate(
                        defaultBuildTaskName,
                        defaultSetupBuildTaskName,
                        defaultCinteropTaskName
                    )
                }
            }
        }
    }

    @DisplayName("UTD with adding and removing target")
    @GradleTest
    fun testUTDTargetAdded(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(defaultPodName, produceGitBlock())

            testImport()

            buildGradleKts.addCocoapodsBlock("osx.deploymentTarget = \"10.15\"")
            testImport()

            val tasks = listOf(
                podspecTaskName,
                defaultPodGenTaskName,
                defaultSetupBuildTaskName,
                defaultBuildTaskName,
                defaultCinteropTaskName
            )
            val anotherTarget = "MacosX64"
            val anotherSdk = "macosx"
            val anotherFamily = "macos"
            buildGradleKts.addKotlinBlock(anotherTarget.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "()")

            testImport {
                assertTasksExecuted(
                    podGenFullTaskName(anotherFamily),
                    podSetupBuildFullTaskName(sdkName = anotherSdk),
                    podBuildFullTaskName(sdkName = anotherSdk),
                    cinteropFullTaskName(targetName = anotherTarget)
                )
                assertTasksUpToDate(tasks)
            }

            buildGradleKts.replaceText(anotherTarget.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "()", "")
            testImport {
                assertOutputDoesNotContain(podGenFullTaskName(anotherFamily))
                assertOutputDoesNotContain(podSetupBuildFullTaskName(sdkName = anotherSdk))
                assertOutputDoesNotContain(podBuildFullTaskName(sdkName = anotherSdk))
                assertOutputDoesNotContain(cinteropFullTaskName(targetName = anotherTarget))
                assertTasksUpToDate(tasks)
            }
        }
    }

    @DisplayName("UTD build")
    @GradleTest
    fun testUTDBuild(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(defaultPodName, produceGitBlock())

            testImport {
                assertTasksExecuted(defaultBuildTaskName)
            }

            val anotherTarget = "MacosX64"
            val anotherSdk = "macosx"
            val anotherSdkDefaultPodTaskName = podBuildFullTaskName(sdkName = anotherSdk)
            buildGradleKts.addCocoapodsBlock("osx.deploymentTarget = \"10.15\"")
            buildGradleKts.addKotlinBlock(anotherTarget.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "()")

            testImport {
                assertTasksUpToDate(defaultBuildTaskName)
                assertTasksExecuted(anotherSdkDefaultPodTaskName)
            }
            testImport {
                assertTasksUpToDate(defaultBuildTaskName, anotherSdkDefaultPodTaskName)
            }
        }
    }

    @DisplayName("Pod Build UTD after clean")
    @GradleTest
    fun testPodBuildUTDClean(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(defaultPodName, produceGitBlock())

            testImport {
                assertTasksExecuted(defaultBuildTaskName)
            }
            build(":clean")
            testImport {
                assertTasksExecuted(defaultBuildTaskName)
            }
        }
    }

    @DisplayName("Link with use of dynamic framework ")
    @GradleTest
    fun testUseDynamicFramework(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(defaultPodName, produceGitBlock())
            buildGradleKts.addFrameworkBlock("isStatic = false")

            build("linkPodDebugFrameworkIOS") {
                val framework = projectPath.resolve("build/bin/iOS/podDebugFramework/cocoapods.framework/cocoapods")
                val processRunResult = runProcess(
                    listOf("file", framework.absolutePathString()),
                    workingDir = projectPath.toFile(),
                    environmentVariables = environmentVariables.environmentalVariables
                )
                assertProcessRunResult(processRunResult) {
                    assertTrue(isSuccessful)
                    assertTrue(output.contains("dynamically linked shared library"))
                }
            }
        }
    }

    @DisplayName("Link with use of static framework ")
    @GradleTest
    fun testUseStaticFramework(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(defaultPodName, produceGitBlock())
            buildGradleKts.addFrameworkBlock("isStatic = true")

            build("linkPodDebugFrameworkIOS") {
                val framework = projectPath.resolve("build/bin/iOS/podDebugFramework/cocoapods.framework/cocoapods")
                val processRunResult = runProcess(
                    listOf("file", framework.absolutePathString()),
                    workingDir = projectPath.toFile(),
                    environmentVariables = environmentVariables.environmentalVariables
                )
                assertProcessRunResult(processRunResult) {
                    assertTrue(isSuccessful)
                    assertTrue(output.contains("current ar archive"))
                }
            }
        }
    }

    @DisplayName("UTD with different spec repo")
    @GradleTest
    fun testUTDPodGen(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            buildGradleKts.addPod(defaultPodName)
            val repos = listOf(
                "https://github.com/alozhkin/spec_repo_example",
                "https://github.com/alozhkin/spec_repo_example_2"
            )

            isRepoAvailable(repos)

            build(defaultPodGenTaskName) {
                assertTasksExecuted(defaultPodGenTaskName)
            }

            buildGradleKts.addSpecRepo("https://github.com/alozhkin/spec_repo_example")
            build(defaultPodGenTaskName) {
                assertTasksExecuted(defaultPodGenTaskName)
            }

            buildGradleKts.addSpecRepo("https://github.com/alozhkin/spec_repo_example_2")
            build(defaultPodGenTaskName) {
                assertTasksExecuted(defaultPodGenTaskName)
            }

            build(defaultPodGenTaskName) {
                assertTasksUpToDate(defaultPodGenTaskName)
            }
        }

    }

    private fun doTestGit(
        gradleVersion: GradleVersion,
        projectName: String = templateProjectName,
        repo: String = defaultPodRepo,
        pod: String = defaultPodName,
        branch: String? = null,
        commit: String? = null,
        tag: String? = null,
        isGradleBuildScript: Boolean = false,
        testImportAssertions: BuildResult.() -> Unit = {},
        block: TestProject.() -> Unit = {},
    ) {

        nativeProjectWithCocoapodsAndIosAppPodFile(projectName, gradleVersion) {
            val buildScript = if (isGradleBuildScript) buildGradle else buildGradleKts
            buildScript.addPod(pod, produceGitBlock(repo, branch, commit, tag))

            testImport(listOf(repo)) {
                podImportAsserts(buildScript)
                testImportAssertions()
            }
            block()
        }

    }

    private fun produceGitBlock(
        repo: String = defaultPodRepo,
        branchName: String? = null,
        commitName: String? = null,
        tagName: String? = null,
    ): String {
        val branch = if (branchName != null) "branch = \"$branchName\"" else ""
        val commit = if (commitName != null) "commit = \"$commitName\"" else ""
        val tag = if (tagName != null) "tag = \"$tagName\"" else ""
        return """source = git("$repo") {
                      |    $branch
                      |    $commit
                      |    $tag
                      |}
                    """.trimMargin()
    }

    private fun BuildResult.podImportAsserts(
        buildScript: Path,
        projectName: String? = null,
    ) {

        val buildScriptText = buildScript.readText()
        val taskPrefix = projectName?.let { ":$it" } ?: ""
        val podspec = "podspec"

        if ("noPodspec()" in buildScriptText) {
            assertTasksSkipped("$taskPrefix:$podspec")
        }

        if ("podfile" in buildScriptText) {
            assertTasksExecuted("$taskPrefix$podInstallTaskName")
        } else {
            assertTasksSkipped("$taskPrefix$podInstallTaskName")
        }
        if (buildScriptText.matches("pod\\(.*\\)".toRegex())) {
            assertTasksExecuted(listOf("$taskPrefix:$POD_GEN_TASK_NAME"))
        }

        with(listOf(POD_SETUP_BUILD_TASK_NAME, POD_BUILD_TASK_NAME).map { "$taskPrefix:$it" }) {
            if (buildScriptText.matches("pod\\(.*\\)".toRegex())) {
                assertTasksExecuted(this)
            }
        }
    }

    private fun isRepoAvailable(repos: List<String>) = runBlocking {
        HttpClient(CIO).use { client ->
            val nonAvailableRepos = repos
                .map { repo ->
                    async { repo to runCatching { client.get(repo).status }.recover { it }.getOrNull() }
                }
                .awaitAll()
                .filter { (_, status) -> status != HttpStatusCode.OK }
            Assumptions.assumeTrue(nonAvailableRepos.isEmpty()) {
                "The following repositories of ${repos.joinToString()} are not available: ${nonAvailableRepos.joinToString()}"
            }
        }
    }

    private fun TestProject.testImport(
        repos: List<String> = listOf(defaultPodRepo),
        vararg args: String,
        assertions: BuildResult.() -> Unit = {},
    ) {

        isRepoAvailable(repos)

        build(podImportTaskName, *args) {
            assertions()
        }
    }


}