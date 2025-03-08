/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.jvm.JvmKotlinp
import org.jetbrains.kotlin.kotlinp.jvm.readKotlinClassHeader
import org.jetbrains.kotlin.test.backend.handlers.JvmBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.org.objectweb.asm.ClassReader
import java.io.File
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.UnstableMetadataApi

class CompareMetadataHandler(
    testServices: TestServices,
    private val compareWithTxt: Boolean = true,
    private val extension: String = ".txt",
    private val verbose: Boolean = false,
) : JvmBinaryArtifactHandler(testServices) {
    private val dumper = MultiModuleInfoDumper()
    private val dumper2 = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val kotlinp = JvmKotlinp(Settings(isVerbose = verbose, sortDeclarations = true))

        val dump = dumper.builderForModule(module)

        // To check that kotlin-metadata-jvm correctly writes everything, we write metadata with it, then read and render it again.
        val dump2 = dumper2.builderForModule(module)

        for (outputFile in info.classFileFactory.asList().sortedBy { File(it.relativePath).nameWithoutExtension }) {
            val path = outputFile.relativePath
            @OptIn(UnstableMetadataApi::class)
            when {
                path.endsWith(".class") -> {
                    val metadata = ClassReader(outputFile.asByteArray().inputStream()).readKotlinClassHeader()!!
                    val classFile = KotlinClassMetadata.readStrict(metadata)
                    val classFile2 = KotlinClassMetadata.readStrict(classFile.write())
                    for ((sb, classFileToRender) in listOf(dump to classFile, dump2 to classFile2)) {
                        sb.appendFileName(path)
                        sb.append(kotlinp.printClassFile(classFileToRender))
                    }
                }
                path.endsWith(".kotlin_module") -> {
                    val moduleFile = KotlinModuleMetadata.read(outputFile.asByteArray())
                    val moduleFile2 = KotlinModuleMetadata.read(moduleFile.write())
                    for ((sb, moduleFileToRender) in listOf(dump to moduleFile, dump2 to moduleFile2)) {
                        sb.appendFileName(path)
                        sb.append(kotlinp.printModuleFile(moduleFileToRender))
                    }
                }
                else -> {
                    assertions.fail { "Unknown file: $outputFile" }
                }
            }
        }
    }

    private fun StringBuilder.appendFileName(path: String) {
        appendLine("// $path")
        appendLine("// ------------------------------------------")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val dump = dumper.generateResultingDump()

        if (compareWithTxt) {
            val sourceFile = testServices.moduleStructure.originalTestDataFiles.first()
            val defaultTxtFile = sourceFile.withExtension(extension)
            val firTxtFile = sourceFile.withExtension(".fir$extension")
            val isFir = testServices.defaultsProvider.frontendKind == FrontendKinds.FIR
            val actualFile = firTxtFile.takeIf { isFir && it.exists() } ?: defaultTxtFile

            assertions.assertEqualsToFile(actualFile, dump)
        }

        if (!testServices.moduleStructure.allDirectives.contains(KotlinpTestDirectives.NO_READ_WRITE_COMPARE)) {
            val dump2 = dumper2.generateResultingDump()
            assertions.assertEquals(dump, dump2) {
                "Dump after a write-read transformation differs from the original. Most likely it means that writing some metadata " +
                        "fields is not supported in kotlin-metadata-jvm."
            }
        }
    }
}