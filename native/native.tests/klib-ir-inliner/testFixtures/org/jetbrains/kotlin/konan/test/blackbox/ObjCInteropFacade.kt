/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.objcinterop

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_CINTEROP_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.invokeCInterop
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangMode
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClangToStaticLibrary
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.konan.test.klib.CustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.klib.customNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.SourcesKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ModuleStructureExtractor.Companion.CINTEROP_SOURCE_EXTENSIONS
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceFileProvider
import kotlin.collections.flatMap
import kotlin.io.extension

/*
 * When [customClassLoader] is not null, it means ObjCInteropFacade is used in scope of forward compatibility testing:
 */
class ObjCInteropFacade(
    val testServices: TestServices,
    val isForwardTest: Boolean = false,
    customClassLoader: ClassLoader? = null
) : AbstractTestFacade<ResultingArtifact.Source, BinaryArtifacts.KLib>() {

    override val inputKind = SourcesKind
    override val outputKind = ArtifactKinds.KLib
    private val settings = testServices.testRunSettings
    private val targets: KotlinNativeTargets = settings.get()
    private val classLoader: ClassLoader = customClassLoader
        ?: settings.get<KotlinNativeClassLoader>().classLoader

    override fun shouldTransform(module: TestModule): Boolean {
        return module.files.any { it.name.endsWith(".def") }
    }

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): BinaryArtifacts.KLib {
        // the following code mimics `CInteropCompilation.result`
        val sourceFileProvider = testServices.sourceFileProvider
        val sourceFiles = module.files.map { sourceFileProvider.getOrCreateRealFileForSourceFile(it) }

        val defFiles = sourceFiles.filter { it.name.endsWith(".def") }
        val defFile = defFiles.singleOrNull() ?: error("Only one .def file is allowed: ${defFiles.map { it.name }}")
        val defRealFileFolder = defFile.parentFile
        val cSourceFiles = sourceFiles.filter {
            it.name.substringAfterLast(".") in CINTEROP_SOURCE_EXTENSIONS
        }

        val expectedArtifact = KLIB(testServices.klibEnvironmentConfigurator.getKlibArtifactDir(testServices, module.name))

        val staticLibraries = cSourceFiles.map {
            // `compileWithClangToStaticLibrary` invokes `clang` executable from either XCode or Llvm toolchains.
            // Should `clang` be not found, please check Gradle dependencies on the binary folder containing the ` clang ` tool
            compileWithClangToStaticLibrary(
                testRunSettings = settings,
                clangMode = when (it.extension) {
                    "c", "m" -> ClangMode.C
                    "cpp", "mm" -> ClangMode.CXX
                    else -> error("unexpected file extension: $it")
                },
                sourceFiles = listOf(it),
                outputFile = expectedArtifact.klibFile.resolveSibling("${it.nameWithoutExtension}.a"),
                includeDirectories = listOf(defRealFileFolder),
                additionalClangFlags = listOf("-fobjc-arc")
            ).assertSuccess().resultingArtifact.libraryFile
        }
        val freeCInteropArgs = module.directives[FREE_CINTEROP_ARGS]
            .flatMap { it.split(" ") }
            .map { it.replace("\$generatedSourcesDir", defRealFileFolder.parentFile.absolutePath) }

        val args = buildList {
            add("-def")
            add(defFile.canonicalPath)
            add("-target")
            add(targets.testTarget.name)
            add("-nopack")  // without `-nopack`, cinterop will create klib as not `expectedArtifact.klibFile.canonicalPath`, but will add `.klib` suffix
            add("-o")
            add(expectedArtifact.klibFile.canonicalPath)
            add("-no-default-libs")
            addAll(freeCInteropArgs)
            staticLibraries.forEach {
                add("-libraryPath")
                add(it.parentFile.absolutePath)
                add("-staticLibrary")
                add(it.name)
            }
            add("-compiler-option")
            add("-I$defRealFileFolder")
            if (isForwardTest && customNativeCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE) {
                add("-Xkonan-home")
                add(currentCustomNativeCompilerSettings.nativeHome.absolutePath)
                customNativeCompilerSettings.defaultLanguageVersion.let {
                    add("-Xklib-abi-compatibility-level")
                    add("${it.major}.${it.minor}")
                }
                if (!this.contains("-Xccall-mode")) {
                    add("-Xccall-mode")
                    add("direct")
                }
            }
            add("-libraryPath")
            add(expectedArtifact.klibFile.parentFile.absolutePath)
        }

        val loggedCInteropParameters = LoggedData.CInteropParameters(args, defFile)
        val (loggedCall: LoggedData, immediateResult: TestCompilationResult.ImmediateResult<out KLIB>) = try {
            val (exitCode, cinteropOutput, cinteropOutputHasErrors, duration) = invokeCInterop(
                classLoader,
                expectedArtifact.klibFile,
                args.toTypedArray()
            )

            val loggedInteropCall = LoggedData.CompilationToolCall(
                toolName = "CINTEROP",
                input = null,
                parameters = loggedCInteropParameters,
                exitCode = exitCode,
                toolOutput = cinteropOutput,
                toolOutputHasErrors = cinteropOutputHasErrors,
                duration = duration
            )
            val res = if (exitCode != ExitCode.OK || cinteropOutputHasErrors)
                TestCompilationResult.CompilationToolFailure(loggedInteropCall)
            else
                TestCompilationResult.Success(expectedArtifact, loggedInteropCall)

            loggedInteropCall to res
        } catch (unexpectedThrowable: Throwable) {
            val loggedFailure = LoggedData.CompilationToolCallUnexpectedFailure(loggedCInteropParameters, unexpectedThrowable)
            val res = TestCompilationResult.UnexpectedFailure(loggedFailure)

            loggedFailure to res
        }
        expectedArtifact.logFile.writeText(loggedCall.toString())

        return BinaryArtifacts.KLib(immediateResult.assertSuccess().resultingArtifact.klibFile, DiagnosticsCollectorImpl())
    }
}
