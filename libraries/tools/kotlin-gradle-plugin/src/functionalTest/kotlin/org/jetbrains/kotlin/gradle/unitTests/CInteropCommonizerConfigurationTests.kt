/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.targets.native.internal.CommonizerTargetAttribute
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.markConsumable
import org.jetbrains.kotlin.gradle.utils.markResolvable
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class CInteropCommonizerConfigurationTests {

    @Test
    fun `test - compatibility rule - superset is compatible`() {
        val project = buildProjectWithMPP()

        val consumable = project.configurations.create("testElements") { configuration ->
            configuration.markConsumable()
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64
                ).identityString
            )
        }

        val resolvable = project.configurations.create("testDependencies") { configuration ->
            configuration.markResolvable()
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64
                ).identityString
            )
        }

        project.dependencies {
            resolvable(project)
        }

        val resolvedDependencies = resolvable.resolvedConfiguration.lenientConfiguration.allModuleDependencies
        if (resolvedDependencies.isEmpty()) fail("Expected at least one dependency")
        if (resolvedDependencies.size > 1) fail("Expected exactly one dependency. Found: $resolvedDependencies")
        val resolved = resolvedDependencies.single()
        assertEquals(consumable.name, resolved.configuration)
    }

    @Test
    fun `test - disambiguation rule - chooses most specific variant`() {
        val project = buildProjectWithMPP()

        project.configurations.create("testConsumableAll") { configuration ->
            configuration.markConsumable()
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64,
                    KonanTarget.MACOS_X64,
                    KonanTarget.MACOS_ARM64
                ).identityString
            )
        }

        /* More specific as it does not offer macos parts */
        val consumableSpecific = project.configurations.create("testConsumableSpecific") { configuration ->
            configuration.markConsumable()
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64,
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                ).identityString
            )
        }

        val resolvable = project.configurations.create("testDependencies") { configuration ->
            configuration.markResolvable()
            configuration.attributes.attribute(
                CommonizerTargetAttribute.attribute,
                CommonizerTarget(
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64
                ).identityString
            )
        }

        project.dependencies {
            resolvable(project)
        }

        val resolvedDependencies = resolvable.resolvedConfiguration.lenientConfiguration.allModuleDependencies
        if (resolvedDependencies.isEmpty()) fail("Expected at least one dependency")
        if (resolvedDependencies.size > 1) fail("Expected exactly one dependency. Found: $resolvedDependencies")
        val resolved = resolvedDependencies.single()
        assertEquals(consumableSpecific.name, resolved.configuration)
    }
}