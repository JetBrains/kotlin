/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import testApi.testRunner.TestDokkaConfigurationBuilder
import testApi.testRunner.dModule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class InvalidContentModuleAndPackageDocumentationReaderTest : AbstractContextModuleAndPackageDocumentationReaderTest() {

    private val includeA by lazy { temporaryDirectory.resolve("includeA.md").toFile() }
    private val includeB by lazy { temporaryDirectory.resolve("includeB.md").toFile() }

    @BeforeTest
    fun materializeInclude() {
        includeA.writeText(
            """
            Invalid random stuff 
            
            # Module moduleA
            Simple stuff
            """.trimIndent()
        )
        includeB.writeText(
            """
            # Module moduleB
            ###
            """.trimIndent()
        )
    }

    private val configurationBuilderA = TestDokkaConfigurationBuilder().apply {
        moduleName = "moduleA"
    }
    private val configurationBuilderB = TestDokkaConfigurationBuilder().apply {
        moduleName = "moduleB"
    }

    private val sourceSetA by configurationBuilderA.sourceSet {
        includes = listOf(includeA.canonicalPath)
    }

    private val sourceSetB by configurationBuilderB.sourceSet {
        includes = listOf(includeB.canonicalPath)
    }

    private val contextA by lazy {
        DokkaContext.create(
            configuration = configurationBuilderA.build(),
            logger = DokkaConsoleLogger(LoggingLevel.DEBUG),
            pluginOverrides = emptyList()
        )
    }
    private val contextB by lazy {
        DokkaContext.create(
            configuration = configurationBuilderB.build(),
            logger = DokkaConsoleLogger(LoggingLevel.DEBUG),
            pluginOverrides = emptyList()
        )
    }

    private val readerA by lazy { contextA.plugin<InternalKotlinAnalysisPlugin>().querySingle { moduleAndPackageDocumentationReader } }
    private val readerB by lazy { contextB.plugin<InternalKotlinAnalysisPlugin>().querySingle { moduleAndPackageDocumentationReader } }


    @Test
    fun `parsing should fail with a message when documentation is in not proper format`() {
        val exception =
            runCatching { readerA.read(dModule(name = "moduleA", sourceSets = setOf(sourceSetA))) }.exceptionOrNull()
        assertEquals(
            "Unexpected classifier: \"Invalid\", expected either \"Module\" or \"Package\". \n" +
                    "For more information consult the specification: https://kotlinlang.org/docs/dokka-module-and-package-docs.html",
            exception?.message
        )
    }

    @Test
    fun `parsing should fail with a message where it encountered error and why`() {
        val exception =
            runCatching { readerB.read(dModule(name = "moduleB", sourceSets = setOf(sourceSetB))) }.exceptionOrNull()?.message!!

        //I don't want to assert whole message since it contains a path to a temporary folder
        assertTrue(exception.contains("Wrong AST Tree. Header does not contain expected content in "))
        assertTrue(exception.contains("includeB.md"))
        assertTrue(exception.contains("element starts from offset 0 and ends 3: ###"))
    }
}

