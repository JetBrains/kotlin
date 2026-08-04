/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersionInternal
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WasmNpmToolingTest {
    @Test
    fun `verify changes in npm packages update WasmNpmTooling version`(
        @TempDir tempDir: File,
    ) {
        val project = ProjectBuilder.builder().build()
        val objects = project.objects

        fun NpmPackageVersionInternal.reset() {
            name.set("name 1")
            resolvedVersion.set("resolved 1")
            requestedVersion.set("requested 1")
        }

        val npv = objects.NpmPackageVersionInternal {
            it.reset()
        }

        val wasmNpmTooling = project.objects.newInstance<WasmNpmTooling>().apply {
            allDeps.set(listOf(npv))
            defaultInstallationDir.set(tempDir)
        }

        fun npmTooling() = wasmNpmTooling.produceEnv().get()

        val initialVersion = npmTooling().version

        fun testChange(mutate: (npv: NpmPackageVersionInternal) -> Unit) {
            npv.reset()
            mutate(npv)
            val actualVersion = npmTooling().version
            assertNotEquals(actualVersion, initialVersion, "Expect version has changed after NpmPackageVersionInternal is modified")

            assertEquals(tempDir.resolve(actualVersion).invariantSeparatorsPath, npmTooling().dir.invariantSeparatorsPath)
        }

        testChange { npv.name.set("name2") }
        testChange { npv.resolvedVersion.set("resolved 2") }
        testChange { npv.requestedVersion.set("requested 2") }
    }
}
