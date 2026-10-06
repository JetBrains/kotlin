/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.DisabledNodeJsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.PreInstalledNodeJsToolchainService
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@ExperimentalNodeJsToolchainDsl
class NodeJsToolchainManagementDslTest {

    private fun buildProjectAndDsl(): Pair<Project, NodeJsToolchainManagementDsl> =
        buildProject().let { project -> project to NodeJsToolchainManagementDslImpl(project.gradle) }

    private fun registeredService(project: Project) = project.gradle.sharedServices.registrations
        .getByName(nodeJsServiceName)
        .service
        .get()

    @Test
    fun `toolchainService without a class configures the default DefaultNodeJsToolchainService`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService {
            downloadBaseUrl.set("https://example.com/dist")
        }
        registerNodeJsToolchainServiceIfAbsent(project)

        val service = assertIs<DefaultNodeJsToolchainServiceImpl>(registeredService(project))
        assertEquals("https://example.com/dist", service.parameters.downloadBaseUrl.get())
    }

    @Test
    fun `toolchainService with a class selects and configures that service`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService(PreInstalledNodeJsToolchainService::class) {
            nodeJsExecutable.set("/opt/node/bin/node")
        }

        registerNodeJsToolchainServiceIfAbsent(project)

        val service = assertIs<PreInstalledNodeJsToolchainServiceImpl>(registeredService(project))
        assertEquals("/opt/node/bin/node", service.parameters.nodeJsExecutable.get())
    }

    @Test
    fun `disable registers the DisabledNodeJsToolchainService`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.disable()
        registerNodeJsToolchainServiceIfAbsent(project)

        assertIs<DisabledNodeJsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `disable is a shorthand for toolchainService with DisabledNodeJsToolchainService`() {
        val (project, dsl) = buildProjectAndDsl()

        dsl.toolchainService(DisabledNodeJsToolchainService::class) { }
        registerNodeJsToolchainServiceIfAbsent(project)

        assertIs<DisabledNodeJsToolchainServiceImpl>(registeredService(project))
    }

    @Test
    fun `configuring the toolchain service a second time fails`() {
        val (_, dsl) = buildProjectAndDsl()

        dsl.toolchainService { }

        assertFailsWith<IllegalStateException> {
            dsl.disable()
        }
    }

    @Test
    fun `disabling and then configuring the toolchain service fails`() {
        val (_, dsl) = buildProjectAndDsl()

        dsl.disable()

        assertFailsWith<IllegalStateException> {
            dsl.toolchainService(PreInstalledNodeJsToolchainServiceImpl::class) { }
        }
    }
}
