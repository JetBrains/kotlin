/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.test.ApplicationEnvironmentDisposer
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstStageFacade
import org.jetbrains.kotlin.test.klib.ReflectionPackageNameHelperModuleTransformer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.test.util.CompiledLibraryCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory

/**
 * An implementation of [CustomKlibCompilerFirstStageFacade] for Native.
 */
class CustomNativeCompilerFirstStageFacade(testServices: TestServices) : CustomKlibCompilerFirstStageFacade(testServices) {
    override val TestModule.customKlibCompilerDefaultLanguageVersion: LanguageVersion
        get() = customNativeCompilerSettings.defaultLanguageVersion

    // Without this check, the facade would compile a module with no Kotlin sources into an empty KLIB.
    override fun shouldTransform(module: TestModule): Boolean {
        return super.shouldTransform(module) && module.files.none { it.name.endsWith(".def") }
    }

    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices, CompilationStage.FIRST)

    override fun compileKlib(
        module: TestModule,
        customArgs: List<String>,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib =
        if (ReflectionPackageNameHelperModuleTransformer.isHelperModule(module))
            compileReflectionPackageNameHelper(module, customArgs, sources, regularDependencies, friendDependencies, outputKlibPath)
        else
            callCustomCompiler(module, customArgs, sources, regularDependencies, friendDependencies, outputKlibPath)

    /**
     * Every grouped test gets its own copy of the [ReflectionPackageNameHelperModuleTransformer] helper module, while its KLIB only
     * depends on the arguments of the compiler call. Compiling it for every test would double the calls of the custom compiler for
     * single-module tests, so the helper is compiled once per distinct call, and its KLIB is copied to the location of the given test,
     * where the second stage looks for it to leave it out of linking.
     *
     * A failure here is no failure of the test data, but of the test infrastructure, so it's reported as such: muting it, as a frontend
     * error of the test, would silently mute every grouped test.
     */
    private fun compileReflectionPackageNameHelper(
        module: TestModule,
        customArgs: List<String>,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib {
        val outputKlibFile = File(outputKlibPath).absoluteFile
        val compilerCall = listOf(
            customArgs, module.directives[FREE_COMPILER_ARGS], sources.map { File(it).readText() }, regularDependencies, friendDependencies,
        ).joinToString("; ")
        val cachedKlibFile = compiledReflectionPackageNameHelpers.getOrCompile(compilerCall) {
            // The cached KLIB is kept until the end of the test run, and the one of a failed compilation is not cached at all.
            val cacheDirectory = createTempDirectory("reflectionPackageNameHelper").toFile()
            Disposer.register(ApplicationEnvironmentDisposer.ROOT_DISPOSABLE) { cacheDirectory.deleteRecursively() }
            val cachedKlibPath = cacheDirectory.resolve(outputKlibFile.name).path
            try {
                callCustomCompiler(module, customArgs, sources, regularDependencies, friendDependencies, cachedKlibPath).outputFile
            } catch (e: CustomKlibCompilerException) {
                cacheDirectory.deleteRecursively()
                testInfraError("The custom compiler failed to compile the `ReflectionPackageName` helper module: ${e.message}")
            }
        }
        if (cachedKlibFile.isDirectory) cachedKlibFile.copyRecursively(outputKlibFile, overwrite = true)
        else cachedKlibFile.copyTo(outputKlibFile, overwrite = true)
        return BinaryArtifacts.KLib(outputKlibFile, DiagnosticsCollectorImpl())
    }

    private fun callCustomCompiler(
        module: TestModule,
        customArgs: List<String>,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib {
        val regularAndFriendDependencies = regularDependencies + friendDependencies
        val outputKlibFile = File(outputKlibPath).absoluteFile

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            customNativeCompilerSettings.compiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    K2NativeCompilerArguments::produce.cliArgument, "library",
                    K2NativeCompilerArguments::moduleName.cliArgument, module.name,
                    K2NativeCompilerArguments::outputName.cliArgument, outputKlibFile.path,
                    K2NativeCompilerArguments::kotlinHome.cliArgument, customNativeCompilerSettings.nativeHome.absolutePath,
                    K2NativeCompilerArguments::nodefaultlibs.cliArgument,
                    K2NativeCompilerArguments::optIn.cliArgument("kotlin.native.internal.InternalForKotlinNative"),
                    // The helper declares `kotlin.internal.ReflectionPackageName` for the stdlibs older than 2.5 missing it.
                    // Other test modules are not allowed to declare anything in the `kotlin` package, as before.
                    CommonCompilerArguments::allowKotlinPackage.cliArgument
                        .takeIf { ReflectionPackageNameHelperModuleTransformer.isHelperModule(module) },
                ),
                regularAndFriendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::libraries.cliArgument, it)
                },
                friendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::friendModules.cliArgument, it)
                },
                module.directives[FREE_COMPILER_ARGS],
                customArgs,
                sources,
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            return BinaryArtifacts.KLib(outputKlibFile, DiagnosticsCollectorImpl())
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }

    private companion object {
        /** The helper KLIBs compiled so far in this test JVM, by the arguments of the compiler call. */
        val compiledReflectionPackageNameHelpers = CompiledLibraryCache()
    }
}
