/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerEdition.CURRENT
import org.jetbrains.kotlin.klib.KlibCompilerEdition.CUSTOM
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependencies
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependency
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.AfterEach
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URLClassLoader
import kotlin.io.path.createTempDirectory

/**
 * This is a base class for tests with repetitive Kotlin/JS compiler invocations.
 *
 * Examples:
 * - Partial linkage tests:
 *   - Build multiple KLIBs with dependencies between some of them
 *   - Rebuild and substitute some of those KLIBs
 *   - Finally, build a binary and run it
 * - KLIB compatibility tests:
 *   - Build KLIBs with one [KlibCompilerEdition]
 *   - Then build a binary with another [KlibCompilerEdition]
 */
abstract class AbstractJsCompilerInvocationTest(protected val compilerType: CompilerType) {
    enum class CompilerType(val es6Mode: Boolean, val useIc: Boolean) {
        NO_IC(es6Mode = false, useIc = false),
        NO_IC_WITH_ES6(es6Mode = true, useIc = false),
        WITH_IC(es6Mode = false, useIc = true),
    }

    protected val buildDir: File = createTempDirectory().toRealPath().toFile().also { it.mkdirs() }

    @AfterEach
    fun clearArtifacts() {
        buildDir.deleteRecursively()
    }

    class JsTestConfiguration(
        testPath: String,
        override val buildDir: File,
        val compilerType: CompilerType,
    ) : KlibCompilerInvocationTestUtils.TestConfiguration {
        override val testDir: File = File(testPath).absoluteFile
        override val stdlibFile: File get() = File("libraries/stdlib/build/classes/kotlin/js/main").absoluteFile
        override val targetBackend get() = if (compilerType.es6Mode) TargetBackend.JS_IR_ES6 else TargetBackend.JS_IR

        override fun onIgnoredTest() {
            /* Do nothing specific. JUnit 3 does not support programmatic tests muting. */
        }
    }
}

private typealias ModuleName = String

private class ModuleDetails(val name: ModuleName, val outputDir: File) {
    constructor(dependency: Dependency) : this(dependency.moduleName, dependency.libraryFile.parentFile)
}

private fun customCompilerCall(): (PrintStream, Array<String>) -> ExitCode {
    val customCompiler = JsKlibTestSettings.customJsCompiler

    val compilerClass = Class.forName("org.jetbrains.kotlin.cli.js.K2JSCompiler", true, customCompiler.classLoader)
    val entryPoint = compilerClass.getMethod(
        "execFullPathsInMessages",
        PrintStream::class.java,
        Array<String>::class.java
    )

    return { printStream: PrintStream, allCompilerArgs: Array<String> ->
        val result = entryPoint.invoke(compilerClass.getDeclaredConstructor().newInstance(), printStream, allCompilerArgs)
        ExitCode.valueOf(result.toString())
    }
}

private fun currentCompilerCall() = { printStream: PrintStream, args: Array<String> ->
    K2JSCompiler().execFullPathsInMessages(printStream, args)
}

internal class CustomJsCompiler(private val jsHome: CustomJsCompilerArtifacts) {
    private var softClassLoader: SoftReference<URLClassLoader>? = null
    val classLoader: URLClassLoader
        get() {
            return softClassLoader?.get() ?: synchronized(this) {
                softClassLoader?.get() ?: createClassLoader(jsHome).also {
                    softClassLoader = SoftReference(it)
                }
            }
        }
}

private fun createClassLoader(jsHome: CustomJsCompilerArtifacts): URLClassLoader {
    val jsClassPath = setOf(
        jsHome.compilerEmbeddable,
        jsHome.baseStdLib,
    )
        .map { it.toURI().toURL() }
        .toTypedArray()

    return URLClassLoader(jsClassPath, null)
        .apply { setDefaultAssertionStatus(true) }
}

internal class CustomJsCompilerArtifacts(val dir: File, version: String) {
    val compilerEmbeddable: File = dir.resolve("kotlin-compiler-embeddable-$version.jar")
    val baseStdLib: File = dir.resolve("kotlin-stdlib-$version.jar")
    val jsStdLib: File = dir.resolve("kotlin-stdlib-js-$version.klib")
}

internal object JsKlibTestSettings {
    val customJsCompilerArtifacts by lazy {
        val location = System.getProperty("kotlin.internal.js.test.compat.customCompilerArtifactsDir")
        requireNotNull(location) { "Custom compiler location is not specified" }

        val version = System.getProperty("kotlin.internal.js.test.compat.customCompilerVersion")
        requireNotNull(version) { "Custom compiler version is not specified" }

        CustomJsCompilerArtifacts(File(location), version)
    }

    val customJsCompiler by lazy {
        CustomJsCompiler(customJsCompilerArtifacts)
    }
}

internal class JsCompilerInvocationTestBinaryArtifact(
    val mainModuleName: String,
    val boxFunctionFqName: String,
    val jsFiles: List<File>,
) : KlibCompilerInvocationTestUtils.BinaryArtifact

internal class JsCompilerInvocationTestArtifactBuilder(
    private val configuration: AbstractJsCompilerInvocationTest.JsTestConfiguration,
) : KlibCompilerInvocationTestUtils.ArtifactBuilder<JsCompilerInvocationTestBinaryArtifact> {
    override fun buildKlib(
        module: KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest,
        dependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
        compilerArguments: List<String>,
    ) {
        require(compilerEdition == CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val kotlinSourceFilePaths = composeSourceFile(module.sourceDir, module.outputDir)

        // Build KLIB:
        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                K2JSCompilerArguments::outputDir.cliArgument, module.klibFile.parentFile.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, module.moduleInfo.moduleName,
                // Halt on any unexpected warning.
                K2JSCompilerArguments::allWarningsAsErrors.cliArgument,
                // Tests suppress the INVISIBLE_REFERENCE check.
                // However, JS doesn't produce the INVISIBLE_REFERENCE error;
                // As result, it triggers a suppression error warning about the redundant suppression.
                // This flag is used to disable the warning.
                K2JSCompilerArguments::dontWarnOnErrorSuppression.cliArgument
            ),
            dependencies.toCompilerArgs(),
            compilerArguments,
            kotlinSourceFilePaths
        )
    }

    private fun composeSourceFile(moduleSourceDir: File, moduleOutputDir: File): MutableList<String> {
        val kotlinSourceFilePaths = mutableListOf<String>()
        moduleSourceDir.walkTopDown().forEach { sourceFile ->
            if (sourceFile.isFile) when (sourceFile.extension) {
                "kt" -> kotlinSourceFilePaths += sourceFile.absolutePath
                "js" -> {
                    // This is needed to preserve *.js files from test data which are required for tests with `external` declarations:
                    sourceFile.copyTo(moduleOutputDir.resolve(sourceFile.relativeTo(moduleSourceDir)), overwrite = true)
                }
            }
        }
        return kotlinSourceFilePaths
    }

    override fun buildBinary(
        mainModule: Dependency,
        otherDependencies: Dependencies,
    ): JsCompilerInvocationTestBinaryArtifact {
        // The modules in `Dependencies.regularDependencies` are already in topological order.
        // It is important to pass the provided and the produced JS files to Node in exactly the same order.
        val knownModulesInTopologicalOrder: List<ModuleDetails> = buildList {
            otherDependencies.regularDependencies.mapTo(this, ::ModuleDetails)
            this += ModuleDetails(mainModule)
        }
        val knownModuleNames: Set<ModuleName> = knownModulesInTopologicalOrder.mapTo(hashSetOf(), ModuleDetails::name)

        val binariesDir: File = File(configuration.buildDir, BIN_DIR_NAME).also { it.mkdirs() }

        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceJs.cliArgument,
                K2JSCompilerArguments::irPerModule.cliArgument,
                K2JSCompilerArguments::moduleKind.cliArgument, "plain",
                K2JSCompilerArguments::includes.cliArgument(mainModule.libraryFile.absolutePath),
                K2JSCompilerArguments::outputDir.cliArgument, binariesDir.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, MAIN_MODULE_NAME,
                // IMPORTANT: Omitting PL arguments here. The default PL mode should be in effect.
                // "-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=INFO",
                K2JSCompilerArguments::allWarningsAsErrors.cliArgument
            ),
            listOf(
                K2JSCompilerArguments::cacheDirectory.cliArgument,
                configuration.buildDir.resolve("libs-cache").absolutePath
            ).takeIf { configuration.compilerType.useIc },
            otherDependencies.toCompilerArgs(),
            listOf(K2JSCompilerArguments::useEsClasses.cliArgument).takeIf { configuration.compilerType.es6Mode }
        )

        // All JS files produced during the compiler call.
        val producedBinaries: Map<ModuleName, File> = binariesDir.walkTopDown()
            .filter { binaryFile -> binaryFile.extension == "js" }
            .associateBy { binaryFile -> binaryFile.guessModuleName() }

        val unexpectedModuleNames = producedBinaries.keys - knownModuleNames
        check(unexpectedModuleNames.isEmpty()) { "Unexpected module names: $unexpectedModuleNames" }

        val allBinaries: List<File> = buildList {
            knownModulesInTopologicalOrder.forEach { moduleDetails ->
                // A set of JS files directly out of test data (aka "external" JS files) to be used immediately in the application:
                val providedBinaries: List<File> = moduleDetails.outputDir.listFiles()?.filter { it.extension == "js" }.orEmpty()
                addAll(providedBinaries)

                // A JS file produced by the compiler for the given module:
                val producedBinary: File? = producedBinaries[moduleDetails.name]
                producedBinary?.let(::add)
            }
        }

        return JsCompilerInvocationTestBinaryArtifact(
            mainModuleName = MAIN_MODULE_NAME,
            boxFunctionFqName = BOX_FUN_FQN,
            jsFiles = allBinaries,
        )
    }

    private fun Dependencies.toCompilerArgs(): List<String> = buildList {
        if (regularDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::libraries.cliArgument
            this += regularDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath }
        }
        if (friendDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath })
        }
    }

    private fun runCompilerViaCLI(vararg compilerArgs: List<String?>?, compilerEdition: KlibCompilerEdition = CURRENT) {
        val allCompilerArgs = compilerArgs.flatMap { args -> args.orEmpty().filterNotNull() }.toTypedArray()

        val invokeCompiler = when (compilerEdition) {
            CUSTOM -> customCompilerCall()
            CURRENT -> currentCompilerCall()
        }

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            invokeCompiler(printStream, allCompilerArgs)
        }

        if (exitCode != ExitCode.OK)
            throw AssertionError(
                buildString {
                    appendLine("Compiler ($compilerEdition) failure.")
                    appendLine("Exit code = $exitCode.")
                    appendLine("Compiler arguments: ${allCompilerArgs.joinToString(separator = ", ")}")
                    appendLine("Compiler messages:")
                    appendLine("==========")
                    appendLine(compilerXmlOutput.toString(Charsets.UTF_8.name()))
                    appendLine("==========")
                }
            )
    }

    private fun File.guessModuleName(): String {
        return when {
            extension != "js" -> error("Not a JS file: $this")
            name.startsWith("kotlin-") && "stdlib" in name -> "stdlib"
            else -> nameWithoutExtension.removePrefix("kotlin_")
        }
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
        private const val BOX_FUN_FQN = "box"
    }
}

internal object JsCompilerInvocationTestBinaryRunner :
    KlibCompilerInvocationTestUtils.BinaryRunner<JsCompilerInvocationTestBinaryArtifact> {

    override fun runBinary(binaryArtifact: JsCompilerInvocationTestBinaryArtifact) {
        val filePaths = binaryArtifact.jsFiles.map { it.canonicalPath }
        V8JsTestChecker.check(
            filePaths, binaryArtifact.mainModuleName, null,
            binaryArtifact.boxFunctionFqName, "OK", withModuleSystem = false,
        )
    }
}
