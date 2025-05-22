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
import org.jetbrains.kotlin.backend.konan.testUtils.multiplatformProjectDir
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxDatetime
import org.jetbrains.kotlin.native.interop.indexer.buildSwiftApiCall
import org.junit.Before
import org.junit.jupiter.api.Test
import java.io.File
import java.io.StringWriter
import kotlin.test.fail

class GenerateExecutionK1K2Tests(
    private val generator: HeaderGenerator,
    private val klibClassifierApiGenerator: KlibClassifierApiGenerator,
) {

    private val moduleName = "Foo"
    private var files: TempFiles = TempFiles(moduleName)

    @Before
    fun before() {
        files = TempFiles(moduleName)
    }

    @Test
    fun `temp - k1`() {
        val header = generateObjCHeader()
        println(header)

    }

    @Test
    fun `temp - k2`() {
        val header = generateObjCHeader()
        println(header)
    }


    @Test
    fun `temp build project - k1`() {
        //buildTestProject("")
    }

    @Test
    fun `temp build project - k2`() {
        val kotlinCalls = generateKotlinCalls()
        val swiftCalls = generateSwiftCalls()
        buildTestProject(kotlinCalls, swiftCalls)
    }

    private fun generateSwiftCalls(): String {
        val headerSource = generateObjCHeader()
        val headerFile = files.file("Foo.h", headerSource.trimIndent())
        val indexerResult = compileAndIndex(listOf(headerFile), files, moduleName)

        return buildString {
            indexerResult.index.objCProtocols.forEach {
                append(buildSwiftApiCall(it))
                appendLine()
            }
        }
    }

    private fun generateKotlinCalls(): String {
        return klibClassifierApiGenerator.generate(listOf(testLibraryKotlinxDatetime))
    }

    private fun generateObjCHeader(): String = generateHeader(
        dependenciesDir.resolve("kotlinxDatetime"), configuration = HeaderGenerator.Configuration(
            dependencies = listOfNotNull(testLibraryKotlinxDatetime),
            exportedDependencies = setOf(testLibraryKotlinxDatetime)
        )
    )

    fun generateHeader(root: File, configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration()): String {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        return generator.generateHeaders(root, configuration).toString()
        //KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }

    private fun buildTestProject(kotlinCalls: String, swiftCalls: String) {
        val projectDir = File("build/native-multiplatform-test-project")
        val original = multiplatformProjectDir
        original.copyRecursively(projectDir, overwrite = true)

        val klibApiFile = projectDir.resolve("shared/src/commonMain/kotlin/org/jetbrains/testproject/ios/KlibApi.kt")
        val updatedKotlinCalls = klibApiFile.readText().replace("//KLIB_API", kotlinCalls)
        klibApiFile.writeText(updatedKotlinCalls)

        val swiftFile = projectDir.resolve("iosApp/iosApp/ContentView.swift")
        val updatedSwiftCalls = swiftFile.readText().replace("//SWIFT_SHARED_CALLS", swiftCalls)
        swiftFile.writeText(updatedSwiftCalls)

        val outputWriter = StringWriter()
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build", "--configuration-cache", "--parallel", "--build-cache")
            .withGradleVersion("8.3")
            .withDebug(true)
            .forwardStdOutput(outputWriter)
            .build()

        println("Build output:")
        println(outputWriter.toString())
    }
}