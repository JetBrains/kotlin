/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.AbstractKlibABITestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExistingLibraryDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_FILE_NAME
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_MODULE_NAME
import org.jetbrains.kotlin.konan.blackboxtest.support.util.generateBoxFunctionLauncher
import org.jetbrains.kotlin.konan.blackboxtest.support.util.getAbsoluteFile
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
abstract class AbstractNativeKlibABITest : AbstractNativeSimpleTest() {
    protected fun runTest(@TestDataFile testPath: String): Unit = AbstractKlibABITestCase.doTest(
        testDir = getAbsoluteFile(testPath),
        buildDir = buildDir,
        stdlibFile = stdlibFile,
        buildKlib = ::buildKlib,
        buildBinaryAndRun = { _, allDependencies -> buildBinaryAndRun(allDependencies) },
        onNonEmptyBuildDirectory = ::backupDirectoryContents
    )

    private fun buildKlib(moduleName: String, moduleSourceDir: File, moduleDependencies: Collection<File>, klibFile: File) {
        val module = createModule(moduleName)
        moduleSourceDir.walk()
            .filter { file -> file.isFile && file.extension == "kt" }
            .forEach { file -> module.files += TestFile.createCommitted(file, module) }

        val testCase = createTestCase(module, COMPILER_ARGS_FOR_KLIB)

        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = createExistingDependencies(moduleDependencies),
            expectedArtifact = TestCompilationArtifact.KLIB(klibFile),
        )

        compilation.result.assertSuccess() // <-- trigger compilation
    }

    private fun buildBinaryAndRun(allDependencies: Collection<File>) {
        val (sourceDir, outputDir) = AbstractKlibABITestCase.createModuleDirs(buildDir, LAUNCHER_MODULE_NAME)
        val executableFile = outputDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix)

        val module = createModule(LAUNCHER_MODULE_NAME)
        module.files += TestFile.createUncommitted(
            location = sourceDir.resolve(LAUNCHER_FILE_NAME),
            module = module,
            text = generateBoxFunctionLauncher("box")
        )

        val testCase = createTestCase(module, COMPILER_ARGS_FOR_EXECUTABLE)

        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = createExistingDependencies(allDependencies),
            expectedArtifact = Executable(executableFile)
        )

        val compilationResult = compilation.result.assertSuccess() // <-- trigger compilation
        val executable = TestExecutable(executableFile, compilationResult.loggedData)

        runExecutableAndVerify(testCase, executable) // <-- run executable and verify
    }

    private fun createModule(moduleName: String) = TestModule.Exclusive(
        name = moduleName,
        directDependencySymbols = emptySet(), /* Don't need to pass any dependency symbols here.
                                                 Dependencies are already handled by the AbstractKlibABITestCase test case class. */
        directFriendSymbols = emptySet()
    )

    private fun createTestCase(module: TestModule.Exclusive, compilerArgs: TestCompilerArgs) = TestCase(
        id = TestCaseId.Named(module.name),
        kind = TestKind.STANDALONE,
        modules = setOf(module),
        freeCompilerArgs = compilerArgs,
        nominalPackageName = PackageName.EMPTY,
        expectedOutputDataFile = null,
        extras = DEFAULT_EXTRAS
    ).apply {
        initialize(null)
    }

    private fun createExistingDependencies(dependencies: Collection<File>) =
        dependencies.map { ExistingLibraryDependency(TestCompilationArtifact.KLIB(it), Library) }

    private val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir
    private val stdlibFile: File get() = testRunSettings.get<KotlinNativeHome>().dir.resolve("klib/common/stdlib")

    companion object {
        private val COMPILER_ARGS_FOR_KLIB = TestCompilerArgs(
            listOf("-nostdlib") // stdlib is passed explicitly.
        )

        private val COMPILER_ARGS_FOR_EXECUTABLE = TestCompilerArgs(
            COMPILER_ARGS_FOR_KLIB.compilerArgs + "-Xpartial-linkage"
        )

        private val DEFAULT_EXTRAS = WithTestRunnerExtras(TestRunnerType.DEFAULT)

        private const val BACKED_UP_DIRECTORY_PREFIX = "__backup-"

        private fun backupDirectoryContents(directory: File) {
            val filesToBackup = directory.listFiles()?.mapNotNull { file ->
                if (file.isDirectory && file.name.startsWith(BACKED_UP_DIRECTORY_PREFIX)) null else file
            }

            if (!filesToBackup.isNullOrEmpty()) {
                val backupDirectory = directory.resolve("$BACKED_UP_DIRECTORY_PREFIX${System.currentTimeMillis()}__")
                backupDirectory.mkdirs()

                filesToBackup.forEach { file -> file.renameTo(backupDirectory.resolve(file.name)) }
            }
        }
    }
}
