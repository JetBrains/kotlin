/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.KotlinPlaywrightJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.PlaywrightBrowserInstall
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KotlinPlaywrightTestFrameworkWiringTest {

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

        val bundleTask = setup.testDsl.defaultBundleTask.get()
        assertFalse(
            bundleTask.enabled,
            "Expected the bundle task to stay disabled when no browser runners are declared"
        )
        assertTrue(
            bundleTask.requiredNpmDependencies.isEmpty(),
            "Expected no npm dependencies to be contributed while the bundle task is disabled"
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

        val bundleTask = setup.testDsl.defaultBundleTask.get()
        assertTrue(
            bundleTask.enabled,
            "Expected the bundle task to be enabled when a browser runner is declared"
        )

        val expectedBundleDir = setup.project.layout.buildDirectory.dir("kotlinJsTest/dist").get()
        assertEquals(
            expectedBundleDir.asFile,
            bundleTask.outputBundleDir.get().asFile
        )
        assertEquals(
            expectedBundleDir.file("test.html").asFile,
            bundleTask.testHtmlFile.get().asFile
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
                    version = "1.60.0",
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

        val installTask = assertIs<PlaywrightBrowserInstall>(
            setup.project.tasks.getByName("kotlinInstallPlaywrightBrowsers")
        )

        assertNotNull(installTask, "Expected kotlinInstallPlaywrightBrowsers task to be registered")
        assertEquals(
            setOf("chromium", "firefox", "webkit"),
            installTask.browsers.get().toSet()
        )
    }

    @Test
    fun `without runners no playwright install task is registered`() {
        val setup = buildBrowserTestProject {}

        val installTask = setup.project.tasks.findByName("kotlinInstallPlaywrightBrowsers")
        assertNull(installTask, "Expected no kotlinInstallPlaywrightBrowsers task when no runners declared")
    }

}


private class BrowserTestProject(
    val project: ProjectInternal,
    val testDsl: KotlinJsBrowserTestDsl,
) {
    val jsBrowserTestTask: KotlinJsTest
        get() = project.tasks.getByName("jsBrowserTest") as KotlinJsTest
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
