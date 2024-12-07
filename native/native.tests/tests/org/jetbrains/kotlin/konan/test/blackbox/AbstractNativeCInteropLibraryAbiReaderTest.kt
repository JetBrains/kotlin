/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.Location
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.MODULE
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.parseModule
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler.Companion.abiDumpFileExtension
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_CLASSES
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KLIB_ABI_DUMP_EXCLUDED_PACKAGES
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KLIB_ABI_DUMP_NON_PUBLIC_MARKERS
import org.jetbrains.kotlin.test.directives.model.ComposedDirectivesContainer
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser.ParsedDirective
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import java.io.File

// TODO (KT-73377): This kind of tests needs to be rewritten to the Frontend Team test infrastructure, the same way
//  as `AbstractNativeLibraryAbiReaderTest` was. However, this would require implementing a new type of source
//  files - `*.def` files.
@Tag("klib")
@OptIn(ExperimentalLibraryAbiReader::class)
abstract class AbstractNativeCInteropLibraryAbiReaderTest : AbstractNativeSimpleTest() {
    fun runTest(localPath: String) {
        val (sourceFile, dumpFiles) = computeTestFiles(localPath)
        val (moduleName, filters) = parseDirectives(sourceFile)

        val customDependencies: List<ExistingDependency<TestCompilationArtifact.KLIB>> =
            produceCustomDependencies(sourceFile).map(TestCompilationArtifact.KLIB::asLibraryDependency)

        val library = compileToLibrary(
            testCase = generateTestCaseWithSingleFile(
                sourceFile = sourceFile,
                moduleName = moduleName,
            ),
            dependencies = customDependencies.toTypedArray()
        ).resultingArtifact.klibFile

        val libraryAbi = LibraryAbiReader.readAbiInfo(library, filters)

        dumpFiles.entries.forEach { (signatureVersion, dumpFile) ->
            val abiDump = LibraryAbiRenderer.render(
                libraryAbi,
                AbiRenderingSettings(signatureVersion)
            )

            assertEqualsToFile(dumpFile, abiDump)
        }
    }

    private fun produceCustomDependencies(sourceFile: File): List<TestCompilationArtifact.KLIB> {
        val targets: KotlinNativeTargets = testRunSettings.get()

        assumeTrue(targets.hostTarget.family.isAppleFamily) // ObjC tests can run only on Apple targets.

        val defFile = sourceFile.withExtension(".def")
        assertTrue(defFile.isFile) { "Def file does not exist: $defFile" }

        return listOf(
            cinteropToLibrary(
                targets = targets,
                defFile = defFile,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs.EMPTY
            ).assertSuccess().resultingArtifact
        )
    }

    companion object {
        private fun computeTestFiles(localPath: String): Pair<File, Map<AbiSignatureVersion, File>> {
            val sourceFile = getAbsoluteFile(localPath)
            assertEquals("kt", sourceFile.extension) { "Invalid source file: $sourceFile" }
            assertTrue(sourceFile.isFile) { "Source file does not exist: $sourceFile" }

            return sourceFile to AbiSignatureVersion.allSupportedByAbiReader.associateWith { signatureVersion ->
                val dumpFileExtension = abiDumpFileExtension(signatureVersion.versionNumber)
                val dumpFile = sourceFile.withReplacedExtensionOrNull("kt", dumpFileExtension)!!
                assertTrue(dumpFile.isFile) { "Dump file does not exist: $dumpFile" }
                dumpFile
            }
        }

        internal data class FromDirectives(val moduleName: String, val filters: List<AbiReadingFilter>)

        internal fun parseDirectives(sourceFile: File): FromDirectives {
            val directivesParser = RegisteredDirectivesParser(
                ComposedDirectivesContainer(KlibAbiDumpDirectives, TestDirectives),
                JUnit5Assertions
            )
            sourceFile.forEachLine(action = directivesParser::parse)

            val registeredDirectives = directivesParser.build()

            val moduleName = parseModule(
                ParsedDirective(MODULE, registeredDirectives[MODULE]),
                Location(sourceFile)
            ).name

            val excludedPackages = registeredDirectives[KLIB_ABI_DUMP_EXCLUDED_PACKAGES]
            val excludedClasses = registeredDirectives[KLIB_ABI_DUMP_EXCLUDED_CLASSES]
            val nonPublicMarkers = registeredDirectives[KLIB_ABI_DUMP_NON_PUBLIC_MARKERS]

            return FromDirectives(
                moduleName = moduleName,
                filters = listOfNotNull(
                    excludedPackages.ifNotEmpty(AbiReadingFilter::ExcludedPackages),
                    excludedClasses.ifNotEmpty(AbiReadingFilter::ExcludedClasses),
                    nonPublicMarkers.ifNotEmpty(AbiReadingFilter::NonPublicMarkerAnnotations)
                )
            )
        }
    }
}
