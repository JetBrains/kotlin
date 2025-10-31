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
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.HostManager
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
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
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
            defFile = File(DEF_FILE_PATH),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs.EMPTY
        ).assertSuccess().resultingArtifact
        spoilDeprecatedAnnotationsInLibrary(library)

        // For this test it's ok to compile executable in the simplest way, not respecting possible `mode=TWO_STAGE_MULTI_MODULE`
        // KT-66014: Extract this test from usual Native test run, and run it in scope of new test module
        compileToExecutableInOneStage(
            generateTestCaseWithSingleFile(
                sourceFile = File(MAIN_FILE_PATH),
                testKind = TestKind.STANDALONE_NO_TR,
                extras = TestCase.NoTestRunnerExtras("main")
            ),
            library.asLibraryDependency()
        ).assertSuccess()
    }

    private fun spoilDeprecatedAnnotationsInLibrary(klib: TestCompilationArtifact.KLIB) {
        // Move the original library to a different location. The former location will be used for the patched library.
        val originalLibraryFile = KFile(with(klib.klibFile) { parentFile.newDir("__backup__").resolve(name).path })
        val patchedLibraryFile = KFile(klib.klibFile.path)
        patchedLibraryFile.renameTo(originalLibraryFile)

        // Read the original library.
        val oldLibrary = KlibLoader { libraryPaths(originalLibraryFile.path) }.load().librariesStdlibFirst.single()

        // Patch the metadata.
        val patchedMetadata = spoilDeprecatedAnnotationsInMetadata(oldLibrary.metadata)

        // Write the patched library.
        val patchedLibraryTmpDir = KFile(patchedLibraryFile.path + "-tmp")
        buildLibrary(
            natives = emptyList(),
            included = emptyList(),
            linkDependencies = emptyList(),
            metadata = patchedMetadata,
            ir = null, // It will be copied from the original library anyway.
            versions = oldLibrary.versions,
            target = HostManager.host,
            output = patchedLibraryTmpDir.path,
            moduleName = oldLibrary.uniqueName,
            nopack = true,
            shortName = oldLibrary.shortName,
            manifestProperties = oldLibrary.manifestProperties,
        )

        // Unzip the original library.
        val originalLibraryTmpDir = KFile(originalLibraryFile.path + "-tmp")
        originalLibraryFile.unzipTo(originalLibraryTmpDir)

        // Drop the metadata from the original library.
        val originalLibraryMetadataDir = KlibMetadataComponentLayout(originalLibraryTmpDir.path).metadataDir
        originalLibraryMetadataDir.deleteRecursively()

        // Copy the metadata from the patched library.
        val patchedLibraryMetadataDir = KlibMetadataComponentLayout(patchedLibraryTmpDir.path).metadataDir
        patchedLibraryMetadataDir.renameTo(originalLibraryMetadataDir)

        // Zip the resulting library.
        originalLibraryTmpDir.zipDirAs(patchedLibraryFile)
    }

    companion object {
        private const val TEST_DATA_DIR = "native/native.tests/testData/CInterop/KT-59030"
        const val DEF_FILE_PATH = "${TEST_DATA_DIR}/cvectors.def"
        const val MAIN_FILE_PATH = "${TEST_DATA_DIR}/vectors.kt"

        private const val DEPRECATED_CLASS_NAME = "kotlin/Deprecated"
        private const val REPLACE_WITH_ARG = "replaceWith"
        private const val EXPRESSION_ARG = "expression"

        private fun File.newDir(name: String): File = resolve(name).apply { mkdirs() }

        private fun spoilDeprecatedAnnotationsInMetadata(originalMetadata: KlibMetadataComponent): SerializedMetadata {
            // Read the metadata.
            val moduleMetadata = KlibModuleMetadata.read(
                object : KlibModuleMetadata.MetadataLibraryProvider {
                    override val moduleHeaderData get() = originalMetadata.moduleHeaderData
                    override fun packageMetadataParts(fqName: String) = originalMetadata.getPackageFragmentNames(fqName)
                    override fun packageMetadata(fqName: String, partName: String) = originalMetadata.getPackageFragment(fqName, partName)
                }
            )

            // Patch the metadata.
            moduleMetadata.fragments.forEach { fragment ->
                fragment.pkg?.let(this::spoilDeprecatedAnnotationsInMetadataContainer)
                fragment.classes.forEach(this::spoilDeprecatedAnnotationsInMetadataClass)
            }

            // Write back the metadata.
            return with(moduleMetadata.write()) {
                SerializedMetadata(module = header, fragments, fragmentNames, MetadataVersion.INSTANCE.toArray())
            }
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
