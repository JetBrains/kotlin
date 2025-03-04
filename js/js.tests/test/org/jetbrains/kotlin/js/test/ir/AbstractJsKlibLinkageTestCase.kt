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
import org.jetbrains.kotlin.klib.KlibCompilerEdition.LATEST_RELEASE
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependency
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.AfterEach
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URLClassLoader
import kotlin.io.path.createTempDirectory


abstract class AbstractJsKlibLinkageTestCase(protected val compilerType: CompilerType) {
    enum class CompilerType(val useFir: Boolean, val es6Mode: Boolean, val useIc: Boolean) {
        K1_NO_IC(useFir = false, es6Mode = false, useIc = false),
        K1_NO_IC_WITH_ES6(useFir = false, es6Mode = true, useIc = false),
        K1_WITH_IC(useFir = false, es6Mode = false, useIc = true),
        K2_NO_IC(useFir = true, es6Mode = false, useIc = false)
    }

    private val buildDir: File = createTempDirectory().toFile().also { it.mkdirs() }


    @AfterEach
    fun clearArtifacts() {
        buildDir.deleteRecursively()
    }

    protected inner class JsTestConfiguration(testPath: String) : PartialLinkageTestUtils.TestConfiguration {
        override val testDir: File = File(testPath).absoluteFile
        override val buildDir: File get() = this@AbstractJsKlibLinkageTestCase.buildDir
        override val stdlibFile: File get() = File("libraries/stdlib/build/classes/kotlin/js/main").absoluteFile
        override val testModeConstructorParameters = mapOf("isJs" to "true")
        override val targetBackend
            get() = if (compilerType.es6Mode) TargetBackend.JS_IR_ES6 else TargetBackend.JS_IR
        override val isK2: Boolean
            get() = compilerType.useFir

        override fun customizeModuleSources(moduleName: String, moduleSourceDir: File) {
            if (moduleName == MAIN_MODULE_NAME)
                customizeMainModuleSources(moduleSourceDir)
        }

        override fun buildKlib(
            moduleName: String,
            buildDirs: ModuleBuildDirs,
            dependencies: Dependencies,
            klibFile: File,
            compilerEdition: KlibCompilerEdition,
            compilerArguments: List<String>,
        ) = this@AbstractJsKlibLinkageTestCase.buildKlib(moduleName, buildDirs, dependencies, klibFile, compilerEdition, compilerArguments)

        override fun buildBinaryAndRun(mainModule: Dependency, otherDependencies: Dependencies) =
            this@AbstractJsKlibLinkageTestCase.buildBinaryAndRun(mainModule, otherDependencies)

        override fun onNonEmptyBuildDirectory(directory: File) {
            directory.listFiles()?.forEach(File::deleteRecursively)
        }

        override fun onIgnoredTest() {
            /* Do nothing specific. JUnit 3 does not support programmatic tests muting. */
        }
    }

    private fun customizeMainModuleSources(moduleSourceDir: File) {
        // Add the @JsExport annotation to make the box function visible to Node.
        moduleSourceDir.walkTopDown().forEach { file ->
            if (file.extension == "kt") {
                var modified = false
                val lines = file.readLines().map { line ->
                    if (line.startsWith("fun $BOX_FUN_FQN()")) {
                        modified = true
                        "@OptIn(ExperimentalJsExport::class) @JsExport $line"
                    } else
                        line
                }
                if (modified) file.writeText(lines.joinToString("\n"))
            }
        }
    }

    protected abstract fun buildKlib(
        moduleName: String,
        buildDirs: ModuleBuildDirs,
        dependencies: Dependencies,
        klibFile: File,
        compilerEdition: KlibCompilerEdition,
        compilerArguments: List<String>,
    )

    protected fun composeSourceFile(buildDirs: ModuleBuildDirs): MutableList<String> {
        val kotlinSourceFilePaths = mutableListOf<String>()
        buildDirs.sourceDir.walkTopDown().forEach { sourceFile ->
            if (sourceFile.isFile) when (sourceFile.extension) {
                "kt" -> kotlinSourceFilePaths += sourceFile.absolutePath
                "js" -> {
                    // This is needed to preserve *.js files from test data which are required for tests with `external` declarations:
                    sourceFile.copyTo(buildDirs.outputDir.resolve(sourceFile.relativeTo(buildDirs.sourceDir)), overwrite = true)
                }
            }
        }
        return kotlinSourceFilePaths
    }

    private fun buildBinaryAndRun(mainModule: Dependency, otherDependencies: Dependencies) {
        // The modules in `Dependencies.regularDependencies` are already in topological order.
        // It is important to pass the provided and the produced JS files to Node in exactly the same order.
        val knownModulesInTopologicalOrder: List<ModuleDetails> = buildList {
            otherDependencies.regularDependencies.mapTo(this, ::ModuleDetails)
            this += ModuleDetails(mainModule)
        }
        val knownModuleNames: Set<ModuleName> = knownModulesInTopologicalOrder.mapTo(hashSetOf(), ModuleDetails::name)

        val binariesDir: File = File(buildDir, BIN_DIR_NAME).also { it.mkdirs() }

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
                buildDir.resolve("libs-cache").absolutePath
            ).takeIf { compilerType.useIc },
            otherDependencies.toCompilerArgs(),
            listOf(K2JSCompilerArguments::useEsClasses.cliArgument).takeIf { compilerType.es6Mode }
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

        executeAndCheckBinaries(MAIN_MODULE_NAME, allBinaries)
    }

    private fun File.guessModuleName(): String {
        return when {
            extension != "js" -> error("Not a JS file: $this")
            name.startsWith("kotlin-") && "stdlib" in name -> "stdlib"
            else -> nameWithoutExtension.removePrefix("kotlin_")
        }
    }

    protected fun Dependencies.toCompilerArgs(): List<String> = buildList {
        if (regularDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::libraries.cliArgument
            this += regularDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath }
        }
        if (friendDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath })
        }
    }

    protected fun runCompilerViaCLI(vararg compilerArgs: List<String?>?, compilerEdition: KlibCompilerEdition = CURRENT) {
        val allCompilerArgs = compilerArgs.flatMap { args -> args.orEmpty().filterNotNull() }.toTypedArray()

        val invokeCompiler = when (compilerEdition) {
            LATEST_RELEASE -> releasedCompilerCall()
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

    private fun executeAndCheckBinaries(mainModuleName: String, dependencies: Collection<File>) {
        val filePaths = dependencies.map { it.canonicalPath }
        V8JsTestChecker.check(filePaths, mainModuleName, null, BOX_FUN_FQN, "OK", withModuleSystem = false)
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
        private const val BOX_FUN_FQN = "box"
    }
}

private typealias ModuleName = String

private class ModuleDetails(val name: ModuleName, val outputDir: File) {
    constructor(dependency: Dependency) : this(dependency.moduleName, dependency.libraryFile.parentFile)
}

private fun releasedCompilerCall(): (PrintStream, Array<String>) -> ExitCode {
    val releasedCompiler = JsKlibTestSettings.releasedJsCompiler

    val compilerClass = Class.forName("org.jetbrains.kotlin.cli.js.K2JSCompiler", true, releasedCompiler.classLoader)
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

internal class ReleasedJsCompiler(private val jsHome: ReleasedJsArtifactsHome) {
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

private fun createClassLoader(jsHome: ReleasedJsArtifactsHome): URLClassLoader {
    val jsClassPath = setOf(
        jsHome.compilerEmbeddable,
        jsHome.baseStdLib,
    )
        .map { it.toURI().toURL() }
        .toTypedArray()

    return URLClassLoader(jsClassPath, null)
        .apply { setDefaultAssertionStatus(true) }
}

internal class ReleasedJsArtifactsHome(val dir: File, version: String) {
    val compilerEmbeddable: File = dir.resolve("kotlin-compiler-embeddable-$version.jar")
    val baseStdLib: File = dir.resolve("kotlin-stdlib-$version.jar")
    val jsStdLib: File = dir.resolve("kotlin-stdlib-js-$version.klib")
}

internal object JsKlibTestSettings {
    val releasedArtifactHome by lazy {
        val location = System.getProperty("kotlin.internal.js.test.latestReleasedCompilerLocation")
        val version = System.getProperty("kotlin.internal.js.test.releasedCompilerVersion")

        requireNotNull(location) { "Released compiler location is not specified" }
        requireNotNull(version) { "Released compiler version is not specified" }

        ReleasedJsArtifactsHome(File(location), version)
    }

    val releasedJsCompiler by lazy {
        ReleasedJsCompiler(releasedArtifactHome)
    }
}