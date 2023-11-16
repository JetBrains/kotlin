/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.decompiler.konan.K2KotlinNativeMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetadataDecompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

abstract class AbstractSirFromCompiledKotlinTest : AbstractDecompiledKnmStubConsistencyTest() {
    override fun createDecompiler(): KlibMetadataDecompiler<*> {
        return K2KotlinNativeMetadataDecompiler()
    }
}


abstract class AbstractDecompiledKnmStubConsistencyTest : KotlinTestWithEnvironment() {
    abstract fun createDecompiler(): KlibMetadataDecompiler<*>

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            ApplicationEnvironmentDisposer.ROOT_DISPOSABLE,
            KotlinTestUtils.newConfiguration(
                ConfigurationKind.JDK_NO_RUNTIME,
                TestJdkKind.MOCK_JDK,
            ),
            EnvironmentConfigFiles.METADATA_CONFIG_FILES,
        )
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()

        environment.projectEnvironment.environment.registerFileType(
            KlibMetaFileType, KlibMetaFileType.defaultExtension
        )
    }

    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        doTest(testDirectoryPath)
    }

    private fun doTest(testDirectoryPath: Path) {
        val commonKlib = compileCommonKlib(testDirectoryPath)
        val files = getKnmFilesFromKlib(commonKlib)

        val module = SirModule()
        val factory = SirGenerator()
        files.forEach { knmFile ->
            val fileViewProviderForDecompiledFile = K2KotlinNativeMetadataDecompiler().createFileViewProvider(
                knmFile, PsiManager.getInstance(project), physical = false,
            )

            val decompiledFile = KlibDecompiledFile(fileViewProviderForDecompiledFile) { virtualFile ->
                createDecompiler().buildDecompiledTextForTests(virtualFile)
            }

            factory.build(decompiledFile)
        }

        val actual = buildString {
            module.declarations
                .filterIsInstance<SirForeignFunction>()
                .forEach {
                    appendLine("${it.fqName}")
                }
        }

        print(actual) //testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".sir")
    }

    private fun compileCommonKlib(file: Path): File {
        val ktFiles = listOf(file)
        val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib")
        KlibTestUtil.compileCommonSourcesToKlib(
            ktFiles.map(Path::toFile),
            libraryName = "library",
            testKlib,
        )

        return testKlib
    }

    private fun getKnmFilesFromKlib(klib: File): List<VirtualFile> {
        val path = klib.toPath()
        val jarFileSystem = environment.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
        val root = jarFileSystem.refreshAndFindFileByPath(path.absolutePathString() + "!/")!!
        val files = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(
            root,
            { virtualFile -> virtualFile.isDirectory || virtualFile.name.endsWith(KLIB_METADATA_FILE_EXTENSION_WITH_DOT) },
            { virtualFile ->
                if (!virtualFile.isDirectory) {
                    files.addIfNotNull(virtualFile)
                }
                true
            })

        return files
    }
}
