/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.isSimulator
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ObjCFrameworkCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependencyType
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.blackbox.support.util.lipoCreate
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class MacOSLinkerIncludedUniversalBinariesTest : AbstractNativeSimpleTest() {

    @Test
    fun includedUniversalDylib___producesThinArchive() = assertProducesThinImageInFramework(
        includedImageType = ImageType.DYLIB,
        linkingFlags = listOf("-Xstatic-framework"),
        expectedMagic = thinArchiveMagic,
    )

    @Test
    fun includedUniversalDylib___producesThinDylib() = assertProducesThinImageInFramework(
        includedImageType = ImageType.DYLIB,
        linkingFlags = emptyList(),
        expectedMagic = thinMachOMagic,
    )

    @Test
    fun includedUniversalArchive___producesThinArchive() = assertProducesThinImageInFramework(
        includedImageType = ImageType.OBJECT_FILE,
        linkingFlags = listOf("-Xstatic-framework"),
        expectedMagic = thinArchiveMagic,
    )

    @Test
    fun includedUniversalArchive___producesThinDylib() = assertProducesThinImageInFramework(
        includedImageType = ImageType.OBJECT_FILE,
        linkingFlags = emptyList(),
        expectedMagic = thinMachOMagic,
    )

    private val thinArchiveMagic = listOf(0x21, 0x3c, 0x61, 0x72)
    private val thinMachOMagic = listOf(0xfe, 0xed, 0xfa, 0xcf).reversed()

    enum class ImageType(val clangOptions: List<String>) {
        DYLIB(
            clangOptions = listOf("-Xlinker", "-dylib"),
        ),
        OBJECT_FILE(
            clangOptions = listOf("-c"),
        );
    }

    private fun assertProducesThinImageInFramework(
        includedImageType: ImageType,
        linkingFlags: List<String>,
        expectedMagic: List<Int>,
    ) {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily)

        val image = createUniversal(imageType = includedImageType)

        val emptySource = buildDir.resolve("stub.kt")
        assert(emptySource.createNewFile())

        val klibWithIncludedUniversalBinary = ExistingDependency(
            compileToLibrary(
                emptySource,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs(
                    "-include-binary", image.canonicalPath,
                ),
                dependencies = emptyList()
            ),
            TestCompilationDependencyType.IncludedLibrary,
        )

        val frameworkImagePath = ObjCFrameworkCompilation(
            testRunSettings,
            freeCompilerArgs = TestCompilerArgs(
                testRunSettings.get<PipelineType>().compilerFlags + listOf(
                    "-Xbinary=bundleId=stub",
                ) + linkingFlags
            ),
            sourceModules = emptyList(),
            dependencies = listOf(klibWithIncludedUniversalBinary),
            expectedArtifact = TestCompilationArtifact.ObjCFramework(
                buildDir,
                "Kotlin",
            )
        ).result.assertSuccess().resultingArtifact.imagePath

        val actualMagic = FileInputStream(frameworkImagePath).use { stream -> (0..<expectedMagic.size).map { stream.read() } }
        assertEquals(
            expectedMagic,
            actualMagic,
        )
    }

    private fun createUniversal(imageType: ImageType): File {
        val emptySource = buildDir.resolve("stub.c")
        assert(emptySource.createNewFile())

        val configurables = testRunSettings.configurables

        val armImage = compileWithArch(
            inputFile = emptySource,
            arch = "arm64",
            imageType = imageType,
        )
        val otherImage = compileWithArch(
            inputFile = emptySource,
            arch = if (configurables.target.family == Family.OSX || configurables.targetTriple.isSimulator) {
                "x86_64"
            } else {
                // Apple devices SDKs don't support x86_64, but support arm64e.
                "arm64e"
            },
            imageType = imageType,
        )

        val outputImage = buildDir.resolve("output.a")
        if (outputImage.exists()) outputImage.delete()
        return lipoCreate(
            inputFiles = listOf(armImage, otherImage),
            outputFile = outputImage,
        ).assertSuccess().resultingArtifact.libraryFile
    }

    private fun compileWithArch(
        inputFile: File,
        arch: String,
        imageType: ImageType,
    ): File {
        val outputImage = buildDir.resolve("output_$arch")
        compileWithClang(
            sourceFiles = listOf(inputFile),
            outputFile = outputImage,
            additionalClangFlags = imageType.clangOptions + listOf("-arch", arch),
        ).assertSuccess()
        return outputImage
    }

}