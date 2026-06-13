/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NpmVersionsTest {

    /**
     * Hardcoded list of expected dependencies.
     *
     * [NpmVersions] is generated - this is a sanity check to make sure the generated file
     * doesn't accidentally drop required dependencies.
     *
     * The versions are tested in [org.jetbrains.kotlin.gradle.targets.web.KotlinNpmToolingLockFilesTest].
     */
    @Test
    fun `verify NpmVersions contains expected dependencies`() {
        assertEquals(
            listOf(
                "css-loader",
                "karma",
                "karma-chrome-launcher",
                "karma-firefox-launcher",
                "karma-ie-launcher",
                "karma-mocha",
                "karma-opera-launcher",
                "karma-safari-launcher",
                "karma-sourcemap-loader",
                "karma-webpack",
                "kotlin-web-helpers",
                "mini-css-extract-plugin",
                "mocha",
                "sass",
                "sass-loader",
                "source-map-loader",
                "source-map-support",
                "style-loader",
                "@swc/helpers",
                "to-string-loader",
                "typescript",
                "webpack",
                "webpack-cli",
                "webpack-dev-server",
            ).prettyPrinted,
            NpmVersions().allDependencies.map { it.name }.prettyPrinted,
        )
    }

    /**
     * Ensure the Karma version points to Kotlin's Karma fork
     * https://github.com/Kotlin/karma
     */
    @Test
    fun `verify Karma version is resolved git url for Kotlin's fork`() {
        val karmaVersion = NpmVersions().karma.version
        val expectedPrefix = "github:Kotlin/karma#"
        assertTrue(karmaVersion.startsWith(expectedPrefix), "Karma version should start with $expectedPrefix, but was $karmaVersion")
    }
}
