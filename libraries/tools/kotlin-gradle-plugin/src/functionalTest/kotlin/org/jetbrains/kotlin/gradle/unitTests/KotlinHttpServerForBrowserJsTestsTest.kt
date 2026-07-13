/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinHttpServerForBrowserJsTests
import org.jetbrains.kotlin.gradle.util.assertLogContains
import org.jetbrains.kotlin.gradle.util.assertLogDoesNotContain
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.withGradleLogCapture
import org.jetbrains.kotlin.gradle.util.withGradleLogCaptureAndResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinHttpServerForBrowserJsTestsTest {

    private fun Project.findKotlinHttpServerForBrowserJsTestsBuilService(): KotlinHttpServerForBrowserJsTests? {
        val registration = project.gradle.sharedServices.registrations
            .singleOrNull { it.name.startsWith("KotlinHttpServerForBrowserJsTests") }
            ?: return null
        return registration.service.get() as KotlinHttpServerForBrowserJsTests
    }

    @Test
    fun `KotlinHttpServerForBrowserJsTests service is not created when new test pipeline is not requested`() {
        val project = buildProjectWithMPP {
            multiplatformExtension.apply {
                js {
                    browser {
                        // no test requested
                    }
                }
            }
        }
        project.evaluate()

        assertNull(project.findKotlinHttpServerForBrowserJsTestsBuilService())
    }

    @Test
    fun `KotlinHttpServerForBrowserJsTests service is registered when default locations is obtained, but no actual http server started`() {
        val project = buildProjectWithMPP {}

        val logs = withGradleLogCapture {
            project.multiplatformExtension.apply {
                js {
                    browser {
                        test {
                            // accessing to default tests location should trigger registration of http server service
                            it.defaultTestsLocationProvider.get()
                        }
                    }
                }
            }
            project.evaluate()
        }

        assertNotNull(project.findKotlinHttpServerForBrowserJsTestsBuilService())
        logs.assertLogDoesNotContain("HTTP server for js tests started")
    }

    @Test
    fun `accessing default tests location url will trigger creation of http server service`() {
        val project = buildProjectWithMPP {}
        project.multiplatformExtension.apply {
            js {
                browser {
                    test { }
                }
            }
        }
        project.evaluate()

        val jsIrTarget = project.multiplatformExtension.js() as KotlinJsIrTarget


        // create directory simulating that bundle task got executed, so http server can safely start
        val defaultTestLocation = jsIrTarget.browser.test.defaultTestsLocationProvider.get()
        defaultTestLocation.bundleLocation.get().asFile.mkdirs()

        try {
            val (url, logs) = withGradleLogCaptureAndResult { defaultTestLocation.url.get() }
            logs.assertLogContains("HTTP server for js tests started at http://${url.host}:${url.port}/")
        } finally {
            // don't leave http server running
            project.findKotlinHttpServerForBrowserJsTestsBuilService()?.close()
        }
    }

    @Test
    fun `closing not started HTTP server should not cause it instantiation`() {
        val project = buildProjectWithMPP {
            multiplatformExtension.apply {
                js {
                    browser {
                        test {
                            // accessing to default tests location should trigger registration of http server service
                            it.defaultTestsLocationProvider.get()
                        }
                    }
                }
            }
        }
        project.evaluate()

        val service = project.findKotlinHttpServerForBrowserJsTestsBuilService()
        assertNotNull(service, "Build service should be registered")

        val logs = withGradleLogCapture {
            service.close()
        }

        logs.assertLogDoesNotContain("HTTP server for js tests .* has been stopped.".toRegex())
    }


    @Test
    fun `serving the same location on the same prefix is idempotent`() {
        val project = buildProjectWithMPP {
            multiplatformExtension.apply {
                js {
                    browser {
                        test {
                            // accessing to default tests location should trigger registration of http server service
                            it.defaultTestsLocationProvider.get()
                        }
                    }
                }
            }
        }
        project.evaluate()

        val jsIrTarget = project.multiplatformExtension.js() as KotlinJsIrTarget
        val service = project.findKotlinHttpServerForBrowserJsTestsBuilService()
        assertNotNull(service, "Build service should be registered")

        // create directory simulating that bundle task got executed, so http server can safely start
        val defaultTestLocation = jsIrTarget.browser.test.defaultTestsLocationProvider.get()
        val bundleDir = defaultTestLocation.bundleLocation.get()
        bundleDir.asFile.mkdirs()

        service.use { service ->
            val logs = withGradleLogCapture {
                val url1 = service.serve(":jsBrowserTest", bundleDir)
                val url2 = service.serve(":jsBrowserTest", bundleDir)
                assertEquals(url1, url2, "URLs should be the same for idempotent serve calls")
            }

            // Verify that the serving message appears only once
            val servingMessagePattern = "Path .* at prefix :jsBrowserTest is already registered".toRegex()
            logs.assertLogContains(servingMessagePattern, 1)
        }
    }
}
