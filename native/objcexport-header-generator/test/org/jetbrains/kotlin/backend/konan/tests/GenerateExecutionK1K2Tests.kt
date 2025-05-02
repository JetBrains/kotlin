/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.KlibClassifierApiGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.dependenciesDir
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxDatetime
import org.jetbrains.kotlin.backend.konan.testUtils.multiplatformProjectDir
import org.junit.jupiter.api.Test
import java.io.File
import java.io.StringWriter
import kotlin.test.fail

class GenerateExecutionK1K2Tests(
    private val generator: HeaderGenerator,
    private val klibClassifierApiGenerator: KlibClassifierApiGenerator
) {

    @Test
    fun `temp - k1`() {
        val header = generateHeader(
            dependenciesDir.resolve("kotlinxDatetime"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxDatetime),
                exportedDependencies = setOf(testLibraryKotlinxDatetime)
            )
        )
        println(header)

    }

    @Test
    fun `temp - k2`() {
        val header = generateHeader(
            dependenciesDir.resolve("kotlinxDatetime"), configuration = HeaderGenerator.Configuration(
                dependencies = listOfNotNull(testLibraryKotlinxDatetime),
                exportedDependencies = setOf(testLibraryKotlinxDatetime)
            )
        )
        println(header)
    }

    @Test
    fun `temp build project - k1`() {
        buildTestProject("")
    }

    @Test
    fun `temp build project - k2`() {
        buildTestProject(generateKotlinFunctions())
    }

    fun generateKotlinFunctions(): String {
        val calls = klibClassifierApiGenerator.generate(listOf(testLibraryKotlinxDatetime))
        return calls
    }

    fun generateHeader(root: File, configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration()): String {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        return generator.generateHeaders(root, configuration).toString()
        //KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }

    private fun buildTestProject(klibApiSource: String) {
        // 1. Copy test project to temp dir
        val projectDir = File("build/native-multiplatform-test-project")
        //val projectDir = File("build/functionalTest/${System.currentTimeMillis()}")
        val original = multiplatformProjectDir//File("testData/myTestProject")
        original.copyRecursively(projectDir, overwrite = true)

        // 2. Replace TEST_VALUE with the actual expression
        //val sourceFile = projectDir.resolve("src/main/kotlin/Main.kt")
        //val updatedText = sourceFile.readText().replace("TEST_VALUE", expression)
        //sourceFile.writeText(updatedText)

        val klibApiFile = projectDir.resolve("shared/src/commonMain/kotlin/org/jetbrains/testproject/ios/KlibApi.kt")
        val updatedKlibApiFile = klibApiFile.readText().replace("KLIB_API", klibApiSource)
        klibApiFile.writeText(updatedKlibApiFile)

        // 3. Run build using GradleRunner
        val outputWriter = StringWriter()
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build", "--configuration-cache", "--parallel", "--build-cache")
            .withGradleVersion("8.3")
            .withDebug(true)
            .forwardStdOutput(outputWriter)
            .build()

        // 4. Check build result
        // If we reach here without exceptions, the build was successful
        println("Build output:")
        println(outputWriter.toString())
    }
}