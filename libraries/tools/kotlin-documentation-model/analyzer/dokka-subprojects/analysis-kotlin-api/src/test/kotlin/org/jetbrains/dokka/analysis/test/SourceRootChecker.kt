/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.Platform

import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.util.CollectingDokkaConsoleLogger
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceRootChecker {

    val dokkaConfiguration = DokkaConfigurationImpl(
        sourceSets = listOf(
            DokkaSourceSetImpl(
                analysisPlatform = Platform.jvm,
                displayName = "KotlinJvmSourceSet",
                sourceSetID = KotlinJvmTestProject.DEFAULT_SOURCE_SET_ID,
                sourceRoots = setOf(File("/src/d1/d2")),
            ),
            DokkaSourceSetImpl(
                analysisPlatform = Platform.jvm,
                displayName = "KotlinJvmSourceSet2",
                sourceSetID = KotlinJvmTestProject.DEFAULT_SOURCE_SET_ID,
                sourceRoots = setOf(File("/src/d1"), File("/src/d1/d2/d3/../d4")),
            ),
            DokkaSourceSetImpl(
                analysisPlatform = Platform.jvm,
                displayName = "KotlinJvmSourceSet3",
                sourceSetID = KotlinJvmTestProject.DEFAULT_SOURCE_SET_ID,
                sourceRoots = setOf(File("/src/c")),
            ),
        )
    )

    @Test
    @OnlySymbols("K1 supports all source roots including intersected ones")
    fun `pre-generation check should failed if source roots are intersected`() {
        val collectingLogger = CollectingDokkaConsoleLogger()

        val context = DokkaContext.create(
            configuration = dokkaConfiguration,
            logger = collectingLogger,
            pluginOverrides = listOf()
        )

        val (preGenerationCheckResult, checkMessages) = context[CoreExtensions.preGenerationCheck].fold(
            Pair(true, emptyList<String>())
        ) { acc, checker -> checker() + acc }

        val expectedMessage = "Source sets 'KotlinJvmSourceSet' and 'KotlinJvmSourceSet2' have the common source roots: /src/d1/d2, /src/d1, /src/d1/d2/d4. Every Kotlin source file should belong to only one source set (module).".replace('/', File.separatorChar)
        assertFalse(preGenerationCheckResult)
        assertEquals(listOf(expectedMessage), checkMessages)
    }

    @Test
    @OnlyDescriptors("K1 supports all source roots including intersected ones")
    fun `pre-generation check should log warning if source roots are intersected`() {
        val collectingLogger = CollectingDokkaConsoleLogger()

        val context = DokkaContext.create(
            configuration = dokkaConfiguration,
            logger = collectingLogger,
            pluginOverrides = listOf()
        )

        val (preGenerationCheckResult, checkMessages) = context[CoreExtensions.preGenerationCheck].fold(
            Pair(true, emptyList<String>())
        ) { acc, checker -> checker() + acc }
        assertEquals(checkMessages, emptyList())
        assertTrue(collectingLogger.collectedLogMessages.contains("Source sets 'KotlinJvmSourceSet' and 'KotlinJvmSourceSet2' have the common source roots: /src/d1/d2, /src/d1, /src/d1/d2/d4. Every Kotlin source file should belong to only one source set (module).".replace('/', File.separatorChar) + "\nIn Dokka K2 it will be an error. Also, please consider reporting your user case: https://github.com/Kotlin/dokka/issues"))
        assertTrue(preGenerationCheckResult)
    }
}