/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.KlibModuleMetadata.MetadataLibraryProvider
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCInteropArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KlibFormat
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.metadata.KmAnnotation

@Tag("cinterop")
class CInteropDeserializationTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata("unbound-symbol-in-annotation")
    fun `Deserialization of C-interop declaration with unbound symbol in annotation (KT-89401)`() {
        val cinteropKlibDir = compileToCInteropLibrary {
            """
                language = C
                ---
                typedef struct {
                  int x;
                  int y;
                } Point;
            """.trimIndent()
        }

        val mainKlibDir = compileToRegularLibrary(cinteropKlibDir) {
            """
                @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

                fun test(point: cinterop_lib.Point?) {
                    println(point)
                } 

                fun main() {
                    test(null)
                    println("All good!")
                }
            """.trimIndent()
        }

        cinteropKlibDir.patchKlibModuleMetadata { moduleMetadata ->
            // Add some non-existing annotation:
            moduleMetadata.fragments.single().classes.forEach { clazz ->
                clazz.annotations += KmAnnotation("kotlinx/rpc/protobuf/internal/shim/InternalNativeProtobufApi", emptyMap())
            }
        }

        try {
            generateExecutable(includedLibrary = mainKlibDir, cinteropKlibDir)
            fail { "Should have failed" }
        } catch (e: CompilationToolException) {
            assertTrue("java.lang.NullPointerException" in e.reason)
            assertTrue("IrLazilyBoundAnnotationImpl" in e.reason)
        }
    }

    private inline fun compileToCInteropLibrary(defFileContents: () -> String): Path {
        val defFile = sourcesDir.resolve("cinterop_lib.def").apply { writeText(defFileContents()) }

        val libraryDir = cinteropToLibrary(
            defFile,
            outputDir = buildDir,
            TestCInteropArgs("-nopack")
        ).assertSuccess().resultingArtifact.klibFile

        assertTrue(libraryDir.isDirectory)

        return libraryDir.toPath()
    }

    private fun compileToRegularLibrary(vararg dependencies: Path, sourceFileContents: () -> String): Path {
        val sourceFile = sourcesDir.resolve("regular_lib.kt").apply { writeText(sourceFileContents()) }

        val libraryDir = compileLibrary(
            settings = testRunSettings,
            source = sourceFile,
            freeCompilerArgs = buildList {
                for (dependency in dependencies) {
                    this += "-l"
                    this += dependency.absolutePathString()
                }
            },
            packed = false,
        ).assertSuccess().resultingArtifact.klibFile

        assertTrue(libraryDir.isDirectory)

        return libraryDir.toPath()
    }

    private fun generateExecutable(includedLibrary: Path, vararg dependencies: Path) {
        ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            sourceModules = emptyList(),
            extras = NoTestRunnerExtras("main"),
            dependencies = buildList {
                this += ExistingDependency(KLIB(includedLibrary.toFile()), IncludedLibrary)
                for (dependency in dependencies) {
                    this += ExistingDependency(KLIB(dependency.toFile()), Library)
                }
            },
            expectedArtifact = getExecutableArtifact(),
        ).result.assertSuccess()
    }

    private val sourcesDir: File
        get() = buildDir.resolve("sources").apply { mkdirs() }

    @OptIn(ExperimentalPathApi::class)
    private inline fun Path.patchKlibModuleMetadata(block: (KlibModuleMetadata) -> Unit) {
        assertTrue(isDirectory())

        val backupDir = createTempDirectory("klib-patching")
        copyToRecursively(backupDir, followLinks = false, overwrite = false)
        backupDir.resolve("$KLIB_DEFAULT_COMPONENT_NAME/$KLIB_METADATA_FOLDER_NAME").deleteRecursively() // Don't back up the metadata.

        val library = KlibLoader { libraryPaths(this@patchKlibModuleMetadata) }.load().librariesStdlibFirst.single()
        val metadataComponent = library.metadata

        val moduleMetadata = KlibModuleMetadata.readStrict(
            object : MetadataLibraryProvider {
                override val moduleHeaderData get() = metadataComponent.moduleHeaderData
                override val metadataVersion get() = KlibMetadataVersion.LATEST_STABLE_SUPPORTED
                override fun packageMetadataParts(fqName: String) = metadataComponent.getPackageFragmentNames(fqName)
                override fun packageMetadata(fqName: String, partName: String) = metadataComponent.getPackageFragment(fqName, partName)
            }
        )

        block(moduleMetadata)

        val serializedMetadata = moduleMetadata.write()

        KlibWriter {
            format(KlibFormat.Directory)
            includeMetadata(
                SerializedMetadata(
                    serializedMetadata.header,
                    serializedMetadata.fragments,
                    serializedMetadata.fragmentNames,
                    serializedMetadata.metadataVersion.toArray(),
                )
            )
            // We have to have manifest {} block here because the KlibWriter requires it by design.
            // But the manifest will anyway be rewritten with the original copy.
            manifest {
                moduleName(library.moduleName)
                versions(library.versions)
                platformAndTargets(BuiltInsPlatform.NATIVE)
            }
        }.writeTo(this) // Overwrite the existing library.

        // Overwrite everything from the backup except for metadata:
        resolve(KLIB_DEFAULT_COMPONENT_NAME).listDirectoryEntries().forEach {
            if (it.name != KLIB_METADATA_FOLDER_NAME) it.deleteRecursively()
        }
        backupDir.copyToRecursively(this, followLinks = false, overwrite = false)
    }
}
