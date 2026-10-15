/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalNodeJsToolchainDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainService
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.WebpackBundleKotlinJsTests
import org.jetbrains.kotlin.gradle.targets.js.testing.playwright.KotlinPlaywrightJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.DefaultNodeJsToolchainServiceImpl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.DisabledNodeJsToolchainServiceImpl
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.legacyNodeJsExecutable
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.nodeJsServiceName
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.registerNodeJsToolchainServiceIfAbsent
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.requestDefaultNodeJs
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.resolveNodeJsExecutable
import org.jetbrains.kotlin.gradle.tasks.nodejs.UsesNodeJsToolchainService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.gradle.utils.property
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeJsToolchainTaskWiringTest {

    private fun buildJsBrowserProject(preApplyCode: Project.() -> Unit = {}): Project {
        val project = buildProjectWithMPP(preApplyCode = preApplyCode) {
            with(multiplatformExtension) {
                js {
                    browser()
                    binaries.executable()
                }
            }
        }
        project.evaluate()
        return project
    }

    private fun Project.registeredService(): NodeJsToolchainService<*> = gradle.sharedServices.registrations
        .getByName(nodeJsServiceName)
        .service
        .get() as NodeJsToolchainService<*>

    @Test
    fun `default node js request uses the default version and the build platform`() {
        val request = buildProject().requestDefaultNodeJs().get()

        assertEquals("24.16.0", request.version.get().version)
        assertTrue(request.platform.isPresent, "Expected the platform to default to the build platform")
    }

    @Test
    fun `webpack tasks use the node js toolchain service and request node js`() {
        val project = buildJsBrowserProject()

        val webpackTasks = project.tasks.withType(KotlinWebpack::class.java).toList()
        assertTrue(webpackTasks.isNotEmpty(), "Expected at least one KotlinWebpack task")
        webpackTasks.forEach { task ->
            assertIs<UsesNodeJsToolchainService>(task)
            assertTrue(task.nodeJsToolchainService.isPresent, "Expected ${task.name} to have the toolchain service")
            assertEquals("24.16.0", task.nodeJsRequest.get().version.get().version)
        }
    }

    @Test
    fun `webpack bundle task for js tests uses the node js toolchain service`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                js {
                    browser {
                        test.apply { chromium() }
                    }
                }
            }
        }
        project.evaluate()

        val task = project.tasks.withType(WebpackBundleKotlinJsTests::class.java).singleOrNull()
        assertNotNull(task, "Expected the bundle task to be registered once a browser runner is declared")
        assertIs<UsesNodeJsToolchainService>(task)
        assertTrue(task.nodeJsToolchainService.isPresent)
    }

    @Test
    fun `node js exec does not resolve the executable at configuration time`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_JS_NODEJS_TOOLCHAIN, "download")
            }
        ) {
            with(multiplatformExtension) {
                js {
                    nodejs()
                    binaries.executable()
                }
            }
        }
        project.evaluate()
        assertIs<DefaultNodeJsToolchainServiceImpl>(project.registeredService())

        val tasks = project.tasks.withType(NodeJsExec::class.java).toList()
        assertTrue(tasks.isNotEmpty(), "Expected at least one NodeJsExec task")
        tasks.forEach { task ->
            assertIs<UsesNodeJsToolchainService>(task)
            assertNull(task.executable, "Expected ${task.name} to resolve Node.js only at execution time")
            assertTrue(!task.nodeExecutable.isPresent, "Expected no legacy executable when the toolchain is enabled")
            assertEquals("24.16.0", task.nodeJsRequest.get().version.get().version)
        }
    }

    @Test
    fun `playwright framework resolves the legacy executable when the toolchain is disabled`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                js {
                    browser {
                        test.apply { chromium() }
                    }
                }
            }
        }
        project.evaluate()
        assertIs<DisabledNodeJsToolchainServiceImpl>(project.registeredService())

        val framework = assertIs<KotlinPlaywrightJsTestFramework>(
            (project.tasks.getByName("jsBrowserTest") as KotlinJsTest).testFramework
        )
        assertTrue(framework.executable.get().isNotBlank(), "Expected the legacy executable to be resolved")
    }

    @Test
    fun `disabled toolchain resolves the legacy executable`() {
        val project = buildProject()
        registerNodeJsToolchainServiceIfAbsent(project)
        val service = assertIs<DisabledNodeJsToolchainServiceImpl>(project.registeredService())

        val executable = service.resolveNodeJsExecutable(
            project.provider { "/legacy/node" },
            project.requestDefaultNodeJs(),
        )

        assertEquals("/legacy/node", executable)
    }

    @Test
    fun `legacy executable is empty when the toolchain is enabled`() {
        val project = buildJsBrowserProject {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_JS_NODEJS_TOOLCHAIN, "download")
        }
        assertIs<DefaultNodeJsToolchainServiceImpl>(project.registeredService())
        val service: Provider<NodeJsToolchainService<*>> = project.provider { project.registeredService() }
        val compilation = project.multiplatformExtension.js().compilations.getByName("main")

        val legacy = service.legacyNodeJsExecutable(project.objects, compilation)

        assertTrue(!legacy.isPresent, "Expected no legacy executable when the toolchain service is enabled")
    }
}
