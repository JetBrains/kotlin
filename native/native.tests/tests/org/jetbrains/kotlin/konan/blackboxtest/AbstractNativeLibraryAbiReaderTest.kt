/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
@OptIn(ExperimentalLibraryAbiReader::class)
abstract class AbstractNativeLibraryAbiReaderTest : AbstractNativeSimpleTest() {
    fun runTest(localPath: String) {
        val (sourceFile, dumpFiles) = computeTestFiles(localPath)
        val (moduleName, filters) = computeModuleNameAndFiltersFromTestDirectives(sourceFile)

        val library = compileToLibrary(
            generateTestCaseWithSingleFile(
                sourceFile = sourceFile,
                moduleName = moduleName,
                freeCompilerArgs = TestCompilerArgs("-Xcontext-receivers")
            )
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

    companion object {
        private fun computeTestFiles(localPath: String): Pair<File, Map<AbiSignatureVersion, File>> {
            val sourceFile = getAbsoluteFile(localPath)
            assertEquals("kt", sourceFile.extension) { "Invalid source file: $sourceFile" }
            assertTrue(sourceFile.isFile) { "Source file does not exist: $sourceFile" }

            return sourceFile to AbiSignatureVersion.allSupportedByAbiReader.associateWith { signatureVersion ->
                val dumpFile = sourceFile.withReplacedExtensionOrNull("kt", "v${signatureVersion.versionNumber}.txt")!!
                assertTrue(dumpFile.isFile) { "Dump file does not exist: $dumpFile" }
                dumpFile
            }
        }

        private const val DIRECTIVE_MODULE = "// MODULE:"
        private const val DIRECTIVE_EXCLUDED_PACKAGES = "// EXCLUDED_PACKAGES:"
        private const val DIRECTIVE_EXCLUDED_CLASSES = "// EXCLUDED_CLASSES:"
        private const val DIRECTIVE_NON_PUBLIC_MARKERS = "// NON_PUBLIC_MARKERS:"

        internal fun computeModuleNameAndFiltersFromTestDirectives(sourceFile: File): Pair<String, List<AbiReadingFilter>> {
            fun String.parseQualifiedName() = AbiQualifiedName(
                packageName = AbiCompoundName(substringBefore('/', missingDelimiterValue = "")),
                relativeName = AbiCompoundName(substringAfter('/'))
            )

            var moduleName: String? = null
            val excludedPackages = mutableListOf<AbiCompoundName>()
            val excludedClasses = mutableListOf<AbiQualifiedName>()
            val nonPublicMarkers = mutableListOf<AbiQualifiedName>()

            for (line in sourceFile.bufferedReader().lineSequence()) {
                if (!line.parseTestDirective(DIRECTIVE_EXCLUDED_PACKAGES, ::AbiCompoundName, excludedPackages::add)
                    && !line.parseTestDirective(DIRECTIVE_EXCLUDED_CLASSES, String::parseQualifiedName, excludedClasses::add)
                    && !line.parseTestDirective(DIRECTIVE_NON_PUBLIC_MARKERS, String::parseQualifiedName, nonPublicMarkers::add)
                    && !line.parseTestDirective(DIRECTIVE_MODULE, { it }, { moduleName = it })
                    && !line.startsWith("//")
                    && line.isNotBlank()
                ) {
                    break
                }
            }

            assert(moduleName != null) { "No module name specified with MODULE test directive" }

            return moduleName!! to listOfNotNull(
                excludedPackages.ifNotEmpty(AbiReadingFilter::ExcludedPackages),
                excludedClasses.ifNotEmpty(AbiReadingFilter::ExcludedClasses),
                nonPublicMarkers.ifNotEmpty(AbiReadingFilter::NonPublicMarkerAnnotations)
            )
        }

        private inline fun <T> String.parseTestDirective(
            directivePrefix: String,
            parser: (String) -> T,
            consumer: (T) -> Unit,
        ): Boolean {
            if (!startsWith(directivePrefix))
                return false

            val remainder = substring(directivePrefix.length)
            try {
                val items = parseSpaceSeparatedArgs(remainder)
                items.forEach { item -> consumer(parser(item)) }
                return true
            } catch (e: Exception) {
                throw fail<Nothing>("Failure during parsing test directive: $this", e)
            }
        }
    }
}
