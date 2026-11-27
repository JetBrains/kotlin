/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalNodeJsToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.BuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsVersion
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class NodeJsToolchainProvisioningTest {

    @Test
    fun `archive URL of a linux distribution points to a tar gz archive`() {
        assertEquals(
            "https://nodejs.org/dist/v24.22.1/node-v24.22.1-linux-x64.tar.gz",
            nodeJsArchiveUrl("https://nodejs.org/dist", NodeJsVersion("24.22.1"), BuildPlatform("linux", "x64")),
        )
    }

    @Test
    fun `archive URL of a windows distribution points to a zip archive`() {
        assertEquals(
            "https://nodejs.org/dist/v24.22.1/node-v24.22.1-win-x64.zip",
            nodeJsArchiveUrl("https://nodejs.org/dist", NodeJsVersion("24.22.1"), BuildPlatform("win", "x64")),
        )
    }

    @Test
    fun `archive URL trims the trailing slash of the base URL and normalizes the version`() {
        assertEquals(
            "https://mirror.example.com/node/v22.1.0/node-v22.1.0-darwin-arm64.tar.gz",
            nodeJsArchiveUrl("https://mirror.example.com/node/", NodeJsVersion("v22.1.0"), BuildPlatform("darwin", "arm64")),
        )
    }

    @Test
    fun `installation name keeps the layout of the official distributions`() {
        assertEquals(
            "node-v22.1.0-darwin-arm64",
            nodeJsDistributionName(NodeJsVersion("v22.1.0"), BuildPlatform("darwin", "arm64")),
        )
    }

    @Test
    fun `node executable is in the bin directory except on windows`() {
        val installation = File("installation")
        assertEquals(installation.resolve("bin/node"), nodeJsExecutableFile(installation, BuildPlatform("linux", "x64")))
        assertEquals(installation.resolve("node.exe"), nodeJsExecutableFile(installation, BuildPlatform("win", "x64")))
    }
}
