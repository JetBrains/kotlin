/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.AssembleSwiftPackageBinary
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AssembleSwiftPackageBinaryTest {

    @Test
    fun `xcodebuild arguments list one library per slice in order`() {
        val arguments = AssembleSwiftPackageBinary.xcodebuildArguments(
            libraries = listOf(File("/b/fat/ios/libSharedKotlin.a"), File("/b/fat/iosSimulator/libSharedKotlin.a")),
            output = File("/b/binary/SharedKotlin.xcframework"),
        )

        assertEquals(
            listOf(
                "xcodebuild", "-create-xcframework",
                "-library", "/b/fat/ios/libSharedKotlin.a",
                "-library", "/b/fat/iosSimulator/libSharedKotlin.a",
                "-output", "/b/binary/SharedKotlin.xcframework",
            ),
            arguments
        )
    }

    @Test
    fun `xcodebuild arguments for a single slice`() {
        val arguments = AssembleSwiftPackageBinary.xcodebuildArguments(
            libraries = listOf(File("/b/fat/macos/libSharedKotlin.a")),
            output = File("/b/binary/SharedKotlin.xcframework"),
        )

        assertEquals(
            listOf(
                "xcodebuild", "-create-xcframework",
                "-library", "/b/fat/macos/libSharedKotlin.a",
                "-output", "/b/binary/SharedKotlin.xcframework",
            ),
            arguments
        )
    }
}
