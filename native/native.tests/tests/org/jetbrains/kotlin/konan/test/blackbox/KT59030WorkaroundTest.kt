/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.proto.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.*
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode.WithStaticCache
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.MetadataWriterImpl
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

// See KT-59030.
@Tag("partial-linkage")
@EnforcedHostTarget
@UsePartialLinkage(UsePartialLinkage.Mode.ENABLED_WITH_ERROR)
class KT59030WorkaroundTest : AbstractNativeSimpleTest() {
    // This test relies on static caches. So, run it along with other PL tests but only when caches are enabled.
    @BeforeEach
    fun assumeOnlyStaticCacheEverywhere() {
        val cacheMode = testRunSettings.get<CacheMode>()
        assumeTrue(cacheMode is WithStaticCache)
        assumeTrue(cacheMode.useStaticCacheForUserLibraries)
    }

    @Test
    fun kt59030() {
        val library = cinteropToLibrary(
            targets = targets,
            defFile = File(DEF_FILE_PATH),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs.EMPTY
        ).assertSuccess().resultingArtifact
        spoilDeprecatedAnnotationsInLibrary(library)

        compileToExecutable(
            generateTestCaseWithSingleFile(
                sourceFile = File(MAIN_FILE_PATH),
                testKind = TestKind.STANDALONE_NO_TR,
                extras = TestCase.NoTestRunnerExtras("main")
            ),
            library.asLibraryDependency()
        ).assertSuccess()
    }

    private fun spoilDeprecatedAnnotationsInLibrary(klib: TestCompilationArtifact.KLIB) {
        // Make a backup.
        val oldLibraryFile = KFile(with(klib.klibFile) { parentFile.newDir("__backup__").resolve(name).path })
        val newLibraryFile = KFile(klib.klibFile.path)
        newLibraryFile.renameTo(oldLibraryFile)

        // Unzip the new library.
        val newLibraryTmpDir = KFile(newLibraryFile.path + ".tmp")
        oldLibraryFile.unzipTo(newLibraryTmpDir)

        // Read the library.
        val oldLibrary = resolveSingleFileKlib(oldLibraryFile, strategy = ToolingSingleFileKlibResolveStrategy)
        val newLibraryLayout = KotlinLibraryLayoutForWriter(newLibraryFile, newLibraryTmpDir)

        // Patch the library.
        spoilDeprecatedAnnotationsInMetadata(oldLibrary, newLibraryLayout)

        // Zip and clean-up.
        newLibraryTmpDir.zipDirAs(newLibraryFile)
        newLibraryTmpDir.deleteRecursively()
    }

    companion object {
        private const val TEST_DATA_DIR = "kotlin-native/backend.native/tests/interop/basics"
        const val DEF_FILE_PATH = "${TEST_DATA_DIR}/cvectors.def"
        const val MAIN_FILE_PATH = "${TEST_DATA_DIR}/vectors.kt"

        private const val DEPRECATED_CLASS_NAME = "kotlin/Deprecated"
        private const val REPLACE_WITH_ARG = "replaceWith"
        private const val EXPRESSION_ARG = "expression"

        private fun File.newDir(name: String): File = resolve(name).apply { mkdirs() }

        private fun spoilDeprecatedAnnotationsInMetadata(
            oldLibrary: KotlinLibrary,
            newLibraryLayout: KotlinLibraryLayoutForWriter,
        ) {
            // Read the metadata.
            val moduleMetadata = KlibModuleMetadata.read(
                object : KlibModuleMetadata.MetadataLibraryProvider {
                    override val moduleHeaderData get() = oldLibrary.moduleHeaderData
                    override fun packageMetadataParts(fqName: String) = oldLibrary.packageMetadataParts(fqName)
                    override fun packageMetadata(fqName: String, partName: String) = oldLibrary.packageMetadata(fqName, partName)
                }
            )

            // Patch the metadata.
            moduleMetadata.fragments.forEach { fragment ->
                fragment.pkg?.let(this::spoilDeprecatedAnnotationsInMetadataContainer)
                fragment.classes.forEach(this::spoilDeprecatedAnnotationsInMetadataClass)
            }

            // Write back the metadata.
            val serializedMetadata = with(moduleMetadata.write()) {
                SerializedMetadata(module = header, fragments, fragmentNames)
            }

            newLibraryLayout.metadataDir.deleteRecursively() // Drop old metadata.
            MetadataWriterImpl(newLibraryLayout).addMetadata(serializedMetadata) // Write new metadata.
        }

        private fun spoilDeprecatedAnnotationsInMetadataContainer(container: KmDeclarationContainer) {
            container.functions.forEach { spoilDeprecatedAnnotationsInMetadataAnnotationList(it.annotations) }
            container.properties.forEach { spoilDeprecatedAnnotationsInMetadataAnnotationList(it.annotations) }
            container.typeAliases.forEach { spoilDeprecatedAnnotationsInMetadataAnnotationList(it.annotations) }
        }

        private fun spoilDeprecatedAnnotationsInMetadataClass(clazz: KmClass) {
            spoilDeprecatedAnnotationsInMetadataAnnotationList(clazz.annotations)
            clazz.constructors.forEach { spoilDeprecatedAnnotationsInMetadataAnnotationList(it.annotations) }
            spoilDeprecatedAnnotationsInMetadataContainer(clazz)
        }

        private fun spoilDeprecatedAnnotationsInMetadataAnnotationList(annotations: MutableList<KmAnnotation>) {
            annotations.replaceAll { annotation ->
                if (annotation.className == DEPRECATED_CLASS_NAME) spoilDeprecatedAnnotationInMetadata(annotation) else annotation
            }
        }

        private fun spoilDeprecatedAnnotationInMetadata(deprecated: KmAnnotation): KmAnnotation =
            deprecated.copy(
                arguments = deprecated.arguments.mapValues { (argName, argValue) ->
                    if (argName == REPLACE_WITH_ARG) spoilReplaceWithAnnotationInMetadata(argValue.unwrap()).wrap() else argValue
                }
            )

        private fun spoilReplaceWithAnnotationInMetadata(replaceWith: KmAnnotation): KmAnnotation =
            replaceWith.copy(
                arguments = replaceWith.arguments.filterKeys { argName -> argName != EXPRESSION_ARG }
            )

        private fun KmAnnotationArgument<*>.unwrap(): KmAnnotation = (this as KmAnnotationArgument.AnnotationValue).value
        private fun KmAnnotation.wrap(): KmAnnotationArgument.AnnotationValue = KmAnnotationArgument.AnnotationValue(this)
    }
}
