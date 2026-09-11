/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.renderSwiftExportOutputDivergence
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.snapshotDirectory
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SyntheticPackageChangeReport
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwiftExportOutputDivergenceTest {

    @TempDir
    lateinit var root: File

    private fun output(name: String, files: Map<String, String>): File {
        val dir = root.resolve(name)
        files.forEach { (path, content) ->
            dir.resolve(path).apply { parentFile.mkdirs() }.writeText(content)
        }
        return dir
    }

    private fun diff(primary: File, other: File): SyntheticPackageChangeReport.Changes =
        SyntheticPackageChangeReport.diff(snapshotDirectory(primary), snapshotDirectory(other))

    @Test
    fun `identical outputs produce no differences`() {
        val primary = output("ios", mapOf("Shared/Shared.swift" to "public func foo() {}", "SharedBridge/Shared.h" to "int foo(void);"))
        val other = output("macos", mapOf("Shared/Shared.swift" to "public func foo() {}", "SharedBridge/Shared.h" to "int foo(void);"))

        assertTrue(diff(primary, other).isEmpty)
    }

    @Test
    fun `added removed and modified files are rendered as a warning`() {
        val primary = output(
            "ios", mapOf(
                "Shared/Shared.swift" to "public func foo() {}",
                "OnlyPrimary/OnlyPrimary.swift" to "public func onlyPrimary() {}",
            )
        )
        val other = output(
            "macos", mapOf(
                "Shared/Shared.swift" to "public func bar() {}",
                "OnlyOther/OnlyOther.swift" to "public func onlyOther() {}",
            )
        )

        val changes = diff(primary, other)

        assertEquals(
            """
            Swift Export output at $other differs from the output at $primary that the Swift package is generated from. The package will only contain the latter.
              + OnlyOther/OnlyOther.swift (only in $other)
              - OnlyPrimary/OnlyPrimary.swift (missing in $other)
              ~ Shared/Shared.swift (modified)
            """.trimIndent(),
            renderSwiftExportOutputDivergence(primary, other, changes)
        )
    }

    @Test
    fun `a missing other directory reports every primary file as removed`() {
        val primary = output("ios", mapOf("Shared/Shared.swift" to "a", "Other/Other.swift" to "b"))
        val other = root.resolve("absent")

        val changes = diff(primary, other)

        assertEquals(
            """
            Swift Export output at $other differs from the output at $primary that the Swift package is generated from. The package will only contain the latter.
              - Other/Other.swift (missing in $other)
              - Shared/Shared.swift (missing in $other)
            """.trimIndent(),
            renderSwiftExportOutputDivergence(primary, other, changes)
        )
    }
}
