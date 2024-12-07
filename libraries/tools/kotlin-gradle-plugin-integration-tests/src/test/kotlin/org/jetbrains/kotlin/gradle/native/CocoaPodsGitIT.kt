/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native


import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_GEN_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_IMPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SETUP_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SPEC_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.CocoapodsPluginDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Git connected K/N tests with cocoapods")
@NativeGradlePluginTests
@OptIn(EnvironmentalVariablesOverride::class)
class CocoaPodsGitIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
            cocoapodsGenerateWrapper = true
        )
    )

    private val templateProjectName = "native-cocoapods-template"
    private val groovyTemplateProjectName = "native-cocoapods-template-groovy"
    private val outdatedRepoName = "native-cocoapods-outdated-repo"

    private val defaultPodRepo = "https://github.com/ekscrypto/Base64"
    private val defaultPodName = "Base64"
    private val defaultTarget = "IOS"
    private val defaultFamily = "ios"
    private val defaultAppleTarget = "iosSimulator"
    private val privateSpecGitRepo = "privateSpec.git"
    private val privateSpecName = "KMPPrivateSpec"
    private val customPodLibraryName = "cocoapodsLibrary"
    private val cinteropTaskName = ":cinterop"
    private val defaultCinteropTaskName = cinteropTaskName + defaultPodName + defaultTarget

    private val podImportTaskName = ":$POD_IMPORT_TASK_NAME"
    private val podspecTaskName = ":$POD_SPEC_TASK_NAME"
    private val podGenTaskName = ":$POD_GEN_TASK_NAME"
    private val podSetupBuildTaskName = ":$POD_SETUP_BUILD_TASK_NAME"
    private val podBuildTaskName = ":$POD_BUILD_TASK_NAME"

    private val defaultPodInstallSyntheticTaskName = ":podInstallSyntheticIos"
    private val defaultPodGenTaskName = podGenFullTaskName()
    private val defaultBuildTaskName = podBuildFullTaskName()
    private val defaultSetupBuildTaskName = podSetupBuildFullTaskName()

    private fun podGenFullTaskName(familyName: String = defaultFamily) =
        podGenTaskName + familyName.capitalize()

    private fun podSetupBuildFullTaskName(podName: String = defaultPodName, appleTarget: String = defaultAppleTarget) =
        podSetupBuildTaskName + podName.capitalize() + appleTarget.capitalize()

    private fun podBuildFullTaskName(podName: String = defaultPodName, appleTarget: String = defaultAppleTarget) =
        podBuildTaskName + podName.capitalize() + appleTarget.capitalize()

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
        doTestGit(gradleVersion, tag = "1.1.2")
    }

    @DisplayName("Downloading pod from git with specifying commit")
    @GradleTest
    fun testPodDownloadGitCommit(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion, commit = "f0edb29fd723a21ad2208d2a6d51edbf36c03b5f")
    }

    @DisplayName("Downloading pod from git with specifying branch")
    @GradleTest
    fun testPodDownloadGitBranch(gradleVersion: GradleVersion) {
        doTestGit(gradleVersion, branch = "master")
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
            branch = "master",
            commit = "b33c69bd76c18d44a8a4b0e79593b752a6467d8d"
        )
    }


    @DisplayName("Downloading pod from git (tag priority is bigger than branch priority)")
    @GradleTest
    fun testPodDownloadGitBranchAndTag(gradleVersion: GradleVersion) {
        doTestGit(
            gradleVersion,
            tag = "1.1.2",
            branch = "master"
        )
    }

    @DisplayName("Downloading pod from git with specifying tag for groovy build file")
    @GradleTest
    fun testGroovyDownloadAndImport(gradleVersion: GradleVersion) {
        doTestGit(
            gradleVersion,
            groovyTemplateProjectName,
            tag = "1.1.2",
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

            val anotherPodName = "SSZipArchive"
            val anotherPodRepo = "https://github.com/ZipArchive/ZipArchive"
            buildGradleKts.addPod(anotherPodName, produceGitBlock(anotherPodRepo, tagName = "2.5.5"))
            buildGradleKts.addCocoapodsBlock("ios.deploymentTarget = \"16.0\"")
            testImport(repos = listOf(defaultPodRepo, anotherPodRepo)) {

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
            val anotherAppleTarget = "macos"
            val anotherFamily = "macos"
            buildGradleKts.addKotlinBlock(anotherTarget.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "()")

            testImport {
                assertTasksExecuted(
                    podGenFullTaskName(anotherFamily),
                    podSetupBuildFullTaskName(appleTarget = anotherAppleTarget),
                    podBuildFullTaskName(appleTarget = anotherAppleTarget),
                    cinteropFullTaskName(targetName = anotherTarget)
                )
                assertTasksUpToDate(tasks)
            }

            buildGradleKts.replaceText(anotherTarget.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "()", "")
            testImport {
                assertOutputDoesNotContain(podGenFullTaskName(anotherFamily))
                assertOutputDoesNotContain(podSetupBuildFullTaskName(appleTarget = anotherAppleTarget))
                assertOutputDoesNotContain(podBuildFullTaskName(appleTarget = anotherAppleTarget))
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
            val anotherAppleTarget = "macos"
            val anotherTargetDefaultPodTaskName = podBuildFullTaskName(appleTarget = anotherAppleTarget)
            buildGradleKts.addCocoapodsBlock("osx.deploymentTarget = \"10.15\"")
            buildGradleKts.addKotlinBlock(anotherTarget.replaceFirstChar { it.lowercase(Locale.getDefault()) } + "()")

            testImport {
                assertTasksUpToDate(defaultBuildTaskName)
                assertTasksExecuted(anotherTargetDefaultPodTaskName)
            }
            testImport {
                assertTasksUpToDate(defaultBuildTaskName, anotherTargetDefaultPodTaskName)
            }
        }
    }

    @DisplayName("Pod Build is not UTD after clean")
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

    @DisplayName("UTD for spec repos")
    @GradleTest
    fun testSpecReposUTD(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {

            buildGradleKts.addPod("Base64")
            build(defaultPodGenTaskName) {
                assertTasksExecuted(defaultPodGenTaskName)
            }

            buildGradleKts.addSpecRepo("https://github.com/alozhkin/spec_repo_example.git")
            build(defaultPodGenTaskName) {
                assertTasksExecuted(defaultPodGenTaskName)
            }

            build(defaultPodGenTaskName) {
                assertTasksUpToDate(defaultPodGenTaskName)
            }
        }
    }

    @DisplayName("Import subspecs")
    @GradleTest
    fun testImportSubspecs(gradleVersion: GradleVersion) {
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod("SDWebImage/Core")
            buildGradleKts.addPod("SDWebImage/MapKit")
            testImport()
        }
    }

    @DisplayName("Spec repos import")
    @GradleTest
    fun testSpecReposImport(gradleVersion: GradleVersion) {
        val podName = "example"
        val podRepo = "https://github.com/alozhkin/spec_repo"
        nativeProjectWithCocoapodsAndIosAppPodFile(gradleVersion = gradleVersion) {
            buildGradleKts.addPod(podName)
            buildGradleKts.addSpecRepo(podRepo)

            testImport(repos = listOf(podRepo)) {
                podImportAsserts(buildGradleKts)
            }
        }
    }

    @DisplayName("Outdated spec repo")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTest
    fun testOutdatedSpecRepo(
        gradleVersion: GradleVersion,
        @TempDir testPodsHomeDir: Path
    ) {
        nativeProjectWithCocoapodsAndIosAppPodFile(
            outdatedRepoName,
            gradleVersion,
            environmentVariables = EnvironmentalVariables(mapOf("CP_HOME_DIR" to testPodsHomeDir.absolutePathString()))
        ) {
            val podLibrary = projectPath.resolve(customPodLibraryName)
            val privateSpecGit = projectPath.resolve(privateSpecGitRepo)
            val privateSpecGitUri = privateSpecGit.toUri().toString()

            buildGradleKts.addSpecRepo(privateSpecGitUri)

            fun podInstallSynthetic(version: String) {
                buildGradleKts.addPod(customPodLibraryName, "version = \"$version\"")
                build(defaultPodGenTaskName) {
                    assertTasksExecuted(defaultPodGenTaskName)
                }

                build(defaultPodInstallSyntheticTaskName) {
                    assertTasksExecuted(defaultPodInstallSyntheticTaskName)
                }

                buildGradleKts.removePod(customPodLibraryName)
            }

            // Create bare repo
            runShellCommands {
                add(listOf("git", "init", "--bare", privateSpecGit.absolutePathString()))
            }

            // Create master branch in a bare repo
            val workingDir = projectPath.relativeTo(privateSpecGit).pathString
            runShellCommands(privateSpecGit) {
                add(listOf("mkdir", "-p", workingDir))
                add(listOf("git", "--work-tree=$workingDir", "checkout", "--orphan", "master"))
                add(listOf("git", "--work-tree=$workingDir", "add", "../$customPodLibraryName.zip"))
                add(listOf("git", "--work-tree=$workingDir", "commit", "-m", "Initial commit"))
            }

            //Add spec repo and publish version 0.1.0
            runShellCommands(podLibrary.resolve("0.1.0")) {
                add(listOf("pod", "repo", "add", privateSpecName, privateSpecGitUri))
                add(listOf("pod", "repo", "push", privateSpecName, "cocoapodsLibrary.podspec"))
            }

            podInstallSynthetic("0.1.0")

            //Silently publish 0.2.0
            val podLibSpecs = projectPath.resolve(customPodLibraryName).relativeTo(privateSpecGit).pathString
            runShellCommands(privateSpecGit) {
                add(listOf("git", "--work-tree=$workingDir", "add", podLibSpecs))
                add(listOf("git", "--work-tree=$workingDir", "commit", "-m", "Bump to 0.2.0"))
            }

            podInstallSynthetic("0.2.0")
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

            testImport(repos = listOf(repo)) {
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
        taskName: String = podImportTaskName,
        repos: List<String> = listOf(defaultPodRepo),
        vararg args: String,
        assertions: BuildResult.() -> Unit = {},
    ) {

        isRepoAvailable(repos)

        build(taskName, *args) {
            assertions()
        }
    }
}