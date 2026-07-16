/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.file.Directory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.ir.getPwInstallBrowserTaskName
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.WebpackBundleKotlinJsTests
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.KotlinPlaywrightJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PwBrowserKind
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PLAYWRIGHT_VERSION
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PlaywrightBrowserInstall
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PwBrowserKind
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions
import java.io.File
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class KotlinPlaywrightTestFrameworkWiringTest {

    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun `declaring a runner replaces the test framework with playwright`() {
        val setup = buildBrowserTestProject {
            chromium()
        }

        assertIs<KotlinPlaywrightJsTestFramework>(setup.jsBrowserTestTask.testFramework)
    }

    @Test
    fun `without runners the default karma framework is kept and bundle task stays disabled`() {
        val setup = buildBrowserTestProject {}

        assertIs<KotlinKarma>(setup.jsBrowserTestTask.testFramework)

        val bundleTask = setup.webpackBundleTask
        setup.mockJsTestLinkOutput()
        assertFalse(
            bundleTask.browserRunnersDeclared.get(),
            "Expected the bundle task to stay disabled when no browser runners are declared"
        )
        assertTrue(
            bundleTask.requiredNpmDependencies.isEmpty(),
            "Expected no npm dependencies to be contributed while the bundle task is disabled"
        )
        assertFalse(
            bundleTask.onlyIf.isSatisfiedBy(bundleTask),
            "Expected the bundle task to be skipped, as no browser runners are declared"
        )
    }

    @Test
    fun `declaring a both runner and karma, runners win`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                js {
                    browser {
                        testTask {
                            it.useKarma {
                                useChromeHeadless()
                            }
                        }
                        test.apply {
                            chromium()
                        }
                    }
                }
            }
        }
        project.evaluate()

        assertIs<KotlinPlaywrightJsTestFramework>((project.project.tasks.getByName("jsBrowserTest") as KotlinJsTest).testFramework)
    }

    @Test
    fun `declaring a runner enables the bundle task with default conventions`() {
        val setup = buildBrowserTestProject {
            firefox()
        }

        val bundleTask = setup.webpackBundleTask
        setup.mockJsTestLinkOutput()
        assertTrue(
            bundleTask.browserRunnersDeclared.get(),
            "Expected the bundle task to be enabled when a browser runner is declared"
        )
        assertTrue(
            bundleTask.requiredNpmDependencies.isNotEmpty(),
            "Expected the bundle task to require npm dependencies, when task is enabled"
        )
        assertTrue(
            bundleTask.onlyIf.isSatisfiedBy(bundleTask),
            "Expected the bundle task to be executed."
        )

        val expectedBundleDir = setup.project.layout.buildDirectory.dir("kotlinJsTest/dist").get()
        assertEquals(
            expectedBundleDir.asFile,
            bundleTask.outputBundleDir.get().asFile
        )
    }

    @Test
    fun `playwright framework requires playwright-core npm dependency`() {
        val setup = buildBrowserTestProject {
            chromium()
        }

        val framework = assertIs<KotlinPlaywrightJsTestFramework>(setup.jsBrowserTestTask.testFramework)
        assertEquals(
            setOf<RequiredKotlinJsDependency>(
                NpmPackageVersion(
                    name = "playwright-core",
                    version = PLAYWRIGHT_VERSION,
                ),
            ).prettyPrinted,
            framework.requiredNpmDependencies.prettyPrinted
        )
    }

    @Test
    fun `playwright install task browsers match declared runner names`() {
        val setup = buildBrowserTestProject {
            chromium()
            firefox()
            webkit("my-webkit")
        }

        listOf(PwBrowserKind.CHROMIUM, PwBrowserKind.FIREFOX, PwBrowserKind.WEBKIT).forEach {
            val installTask = assertIs<PlaywrightBrowserInstall>(
                setup.project.tasks.getByName(it.getPwInstallBrowserTaskName())
            )

            assertNotNull(installTask, "Expected ${it.getPwInstallBrowserTaskName()} task to be registered to install ${it.browserName} browsers")
        }
    }

    @Test
    fun `without runners no playwright install task is registered`() {
        val setup = buildBrowserTestProject {}

        PwBrowserKind.entries.forEach {
            val installTask = setup.project.tasks.findByName(it.getPwInstallBrowserTaskName())
            assertNull(installTask, "Expected no ${it.getPwInstallBrowserTaskName()} task when no runners declared")
        }
    }

    @Test
    fun `two js targets with playwright runners do not cause duplicate task registration`() {
        // KT-87641: Multiple JS targets with Playwright tests must not fail with DuplicateTaskException
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                js("A") {
                    browser {
                        test.apply {
                            firefox()
                            chromium()
                        }
                    }
                }
                js("B") {
                    browser {
                        test.apply {
                            firefox()
                            chromium()
                        }
                    }
                }
            }
        }
        // Should not throw DuplicateTaskException
        project.evaluate()

        listOf(PwBrowserKind.CHROMIUM, PwBrowserKind.FIREFOX).forEach {
            assertIs<PlaywrightBrowserInstall>(
                project.tasks.getByName(it.getPwInstallBrowserTaskName())
            )
        }
    }

    @Test
    fun `playwright framework task inputs correctly wired`() {
        fun mockLocation(): KotlinJsTestsLocation = object : KotlinJsTestsLocation {
            override val bundleLocation: Provider<Directory> get() = TODO("Mock")
            override val testHtmlFileName: Provider<String> get() = TODO("Mock")
            override val url: Provider<URI> get() = TODO("Mock")
        }

        val mockLocation1 = mockLocation()
        val mockLocation2 = mockLocation()
        val mockLocation3 = mockLocation()
        val mockLocation4 = mockLocation()

        val customChromeExecutable = tempDirectory.resolve("custom-chrome-executable").toFile().apply {
            createNewFile()
        }

        val setup = buildBrowserTestProject {
            testsLocation.set(mockLocation1)
            timeout.set(72L.hours)
            launchEnvironmentVariables.set(mapOf("FOO" to "BAR"))

            chromium("myChrome") {
                it.headless.set(false)
                it.launchArgs.set(listOf("--no-sandbox"))
                it.timeout.set(42L.days)
                it.customBrowserExecutable.set(customChromeExecutable)
            }
            firefox {
                it.testsLocation.set(mockLocation2)
            }
            webkit {
                it.testsLocation.set(mockLocation3)
            }
            webkit("webki2") {
                it.testsLocation.set(mockLocation4)
            }
        }

        val framework = assertIs<KotlinPlaywrightJsTestFramework>(setup.jsBrowserTestTask.testFramework)
        val inputs = framework.frameworkTaskInputs

        assertEquals(1, inputs.chromiumRunners.get().size, "Expected 1 chromium runner")
        val chromeRunner = inputs.chromiumRunners.get().first()
        assertEquals("myChrome", chromeRunner.name.get())
        assertFalse(chromeRunner.headless.get(), "Expected headless to be false for myChrome")
        assertEquals(listOf("--no-sandbox"), chromeRunner.launchArgs.get())
        assertEquals(mockLocation1, chromeRunner.testsLocation.get())
        assertEquals(Duration.ofDays(42), chromeRunner.timeout.get())
        assertEquals(customChromeExecutable, chromeRunner.customBrowserExecutable.get().asFile)
        assertEquals(mapOf("FOO" to "BAR"), chromeRunner.launchEnvironmentVariables.get())

        assertEquals(1, inputs.firefoxRunners.get().size, "Expected 1 firefox runner")
        val firefoxRunner = inputs.firefoxRunners.get().first()
        assertEquals("firefox", firefoxRunner.name.get())
        assertEquals(mockLocation2, firefoxRunner.testsLocation.get())
        assertEquals(Duration.ofHours(72), firefoxRunner.timeout.get(), "Expected default timeout for firefox")
        assertEquals(mapOf("FOO" to "BAR"), firefoxRunner.launchEnvironmentVariables.get())

        assertEquals(2, inputs.webkitRunners.get().size, "Expected 2 webkit runners")
        val webkitRunner = inputs.webkitRunners.get().find { it.name.get() == "webkit" }!!
        assertEquals(mockLocation3, webkitRunner.testsLocation.get())
        assertEquals(mapOf("FOO" to "BAR"), webkitRunner.launchEnvironmentVariables.get())

        val webkit2Runner = inputs.webkitRunners.get().find { it.name.get() == "webki2" }!!
        assertEquals(mockLocation4, webkit2Runner.testsLocation.get())
    }

    // These tests pin the IDE debug contract without launching a real Playwright browser.
    @Test
    fun `playwright debug url uses runtime url`() {
        val setup = buildBrowserTestProject {
            chromium()
        }
        val framework = assertIs<KotlinPlaywrightJsTestFramework>(setup.jsBrowserTestTask.testFramework)
        val location = mockLocation(setup.project, URI("http://localhost:12345/test.html"))
        framework.frameworkTaskInputs.chromiumRunners.get().single().testsLocation.set(location)

        val debugUrl = framework.buildDebugUrl(setup.jsBrowserTestTask)

        assertEquals("http://localhost:12345/test.html", debugUrl.toString().substringBefore("?"))
    }

    @Test
    fun `playwright intellij debug falls back to chromium for firefox runner`() {
        val setup = buildBrowserTestProject {
            firefox("selected") {
                it.launchArgs.set(listOf("-devtools"))
                it.launchEnvironmentVariables.set(mapOf("FIREFOX_DEBUG" to "1"))
            }
        }
        val framework = assertIs<KotlinPlaywrightJsTestFramework>(setup.jsBrowserTestTask.testFramework)
        val location = mockLocation(setup.project, URI("http://localhost:12345/test.html"))
        framework.frameworkTaskInputs.firefoxRunners.get().single().testsLocation.set(location)

        val installTask = assertIs<PlaywrightBrowserInstall>(
            setup.project.tasks.getByName("kotlinInstallPlaywrightBrowsers")
        )
        assertEquals(setOf("firefox"), installTask.browsers.get().toSet())

        setup.jsBrowserTestTask.debug = true

        assertEquals(setOf("firefox", "chromium"), installTask.browsers.get().toSet())

        val npmToolingEnv = setup.project.layout.buildDirectory.dir("test-npm-tooling").get().asFile
        npmToolingEnv.resolve("node_modules/playwright-core/cli.js").apply {
            parentFile.mkdirs()
            writeText("")
        }
        framework.npmToolingEnvDir.set(npmToolingEnv)
        framework.debugPort.set(32123)

        val spec = framework.createTestExecutionSpec(
            task = setup.jsBrowserTestTask,
            launchOpts = setup.project.objects.processLaunchOptions(),
            nodeJsArgs = mutableListOf(),
            debug = setup.jsBrowserTestTask.debug,
        )
        val runner = spec.runners.single()

        assertEquals("selected", runner.name)
        assertEquals(PwBrowserKind.CHROMIUM, runner.browserKind)
        assertEquals(emptyList(), runner.launchArgs)
        assertEquals(emptyMap(), runner.launchEnvironmentVariables)
        assertEquals(32123, runner.debugOptions?.remoteDebuggingPort)
    }
}

private class BrowserTestProject(
    val project: ProjectInternal,
    val testDsl: KotlinJsBrowserTestDsl,
) {
    val jsBrowserTestTask: KotlinJsTest
        get() = project.tasks.getByName("jsBrowserTest") as KotlinJsTest

    val webpackBundleTask: WebpackBundleKotlinJsTests
        get() = project.tasks.getByName("prepareWebpackBundleForKotlinJsTests") as WebpackBundleKotlinJsTests

    fun mockJsTestLinkOutput() {
        val jsFile = webpackBundleTask.testsEntryFile.get().asFile
        jsFile.parentFile.mkdirs()
        jsFile.writeText("function xxx() {}")
    }
}

private fun buildBrowserTestProject(configure: KotlinJsBrowserTestDsl.() -> Unit): BrowserTestProject {
    lateinit var testDsl: KotlinJsBrowserTestDsl
    val project = buildProjectWithMPP {
        with(multiplatformExtension) {
            js {
                browser {
                    testDsl = test
                    test(configure)
                }
            }
        }
    }
    project.evaluate()
    return BrowserTestProject(project, testDsl)
}

private fun mockLocation(project: ProjectInternal, uri: URI): KotlinJsTestsLocation = object : KotlinJsTestsLocation {
    override val bundleLocation: Provider<Directory>
        get() = project.layout.buildDirectory.dir("mock-browser-test-bundle")

    override val testHtmlFileName: Provider<String>
        get() = project.providers.provider { "test.html" }

    override val url: Provider<URI>
        get() = project.providers.provider { uri }
}

private fun decodeKotlinTestConfig(uri: URI): String {
    val encodedConfig = uri.rawQuery
        .split("&")
        .single { it.startsWith("kotlinTestConfig=") }
        .substringAfter("=")
    return URLDecoder.decode(encodedConfig, StandardCharsets.UTF_8)
}
