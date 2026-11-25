/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class MergeableFrameworkTest {

    @Test
    fun `framework is not linked as mergeable by default`() {
        assertEquals(emptyList(), frameworkLinkerArguments { })
    }

    @Test
    fun `mergeable framework is linked with make_mergeable`() {
        assertEquals(
            listOf("-make_mergeable"),
            frameworkLinkerArguments { mergeable.set(true) }
        )
    }

    @Test
    fun `mergeable framework keeps user linker options`() {
        assertEquals(
            listOf("-lsqlite3", "-make_mergeable"),
            frameworkLinkerArguments {
                linkerOpts("-lsqlite3")
                mergeable.set(true)
            }
        )
    }

    @Test
    fun `static framework is not linked as mergeable`() {
        assertEquals(
            emptyList(),
            frameworkLinkerArguments {
                isStatic = true
                mergeable.set(true)
            }
        )
    }

    private fun frameworkLinkerArguments(configure: Framework.() -> Unit): List<String> {
        val project = buildProjectWithMPP {
            kotlin {
                iosSimulatorArm64().binaries.framework(configure = configure)
            }
        }.evaluate()

        val linkTask = assertIsInstance<KotlinNativeLink>(project.tasks.getByName("linkReleaseFrameworkIosSimulatorArm64"))
        return linkTask.createCompilerArguments(lenient).singleLinkerArguments.toList()
    }
}
