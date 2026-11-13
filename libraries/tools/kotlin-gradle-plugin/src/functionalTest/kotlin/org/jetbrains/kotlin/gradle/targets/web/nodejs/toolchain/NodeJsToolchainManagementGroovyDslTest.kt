/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * KT-91482: a Groovy closure passed to `toolchainService` must reach the service parameters.
 */
@ExperimentalNodeJsToolchainDsl
class NodeJsToolchainManagementGroovyDslTest {

    @Test
    fun `toolchainService configured from a groovy script applies the settings`() {
        val project = buildProject()
        // Same instantiation as in the ecosystem settings extension: decorated by Gradle.
        project.extensions.add(
            "nodeJsToolchainManagement",
            project.objects.newInstance(NodeJsToolchainManagementDslImpl::class.java, project.gradle)
        )

        val installationDir = project.layout.buildDirectory.dir("node-installations").get().asFile
        val script = project.file("toolchain.gradle")
        script.writeText(
            """
            nodeJsToolchainManagement {
                toolchainService {
                    downloadBaseUrl.set('https://mirror.example.com/dist')
                    installationDir.fileValue(new File('${installationDir.invariantSeparatorsPath}'))
                }
            }
            """.trimIndent()
        )
        project.apply(mapOf("from" to script))
        registerNodeJsToolchainServiceIfAbsent(project)

        val service = assertIs<DefaultNodeJsToolchainServiceImpl>(
            project.gradle.sharedServices.registrations.getByName(nodeJsServiceName).service.get()
        )
        assertEquals("https://mirror.example.com/dist", service.parameters.downloadBaseUrl.get())
        assertEquals(installationDir, service.parameters.installationDir.get().asFile)
    }
}
