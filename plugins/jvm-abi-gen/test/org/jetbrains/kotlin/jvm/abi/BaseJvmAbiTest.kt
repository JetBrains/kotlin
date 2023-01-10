/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class BaseJvmAbiTest : TestCase() {
    private lateinit var workingDir: File

    override fun setUp() {
        super.setUp()
        workingDir = createTempDirectory(javaClass.simpleName).toFile().apply { deleteOnExit() }
    }

    override fun tearDown() {
        workingDir.deleteRecursively()
        super.tearDown()
    }

    private val abiPluginJar = File("dist/kotlinc/lib/jvm-abi-gen.jar")
    private fun abiOption(option: String, value: String): String =
        "plugin:${JvmAbiCommandLineProcessor.COMPILER_PLUGIN_ID}:$option=$value"

    inner class Compilation(
        private val projectDir: File,
        val name: String?,
        val dependencies: Collection<Compilation> = emptyList()
    ) {
        val srcDir: File
            get() = if (name == null) projectDir else projectDir.resolve(name)

        val destinationDir: File
            get() = if (name == null) workingDir.resolve("out") else workingDir.resolve("$name/out")

        val abiDir: File
            get() = if (name == null) workingDir.resolve("abi") else workingDir.resolve("$name/abi")

        val javaDestinationDir: File
            get() = if (name == null) workingDir.resolve("javaOut") else workingDir.resolve("$name/javaOut")

        val directives: File
            get() = projectDir.resolve("directives.txt")

        override fun toString(): String =
            "compilation '$name'"
    }

    fun make(compilation: Compilation) {
        check(abiPluginJar.exists()) { "Plugin jar '$abiPluginJar' does not exist" }
        check(compilation.srcDir.exists()) { "Source dir '${compilation.srcDir}' does not exist" }

        val abiDependencies = compilation.dependencies.map { dep ->
            check(dep.abiDir.exists()) { "Dependency '${dep.name}' of '${compilation.name}' was not built" }
            dep.abiDir
        }

        val directives = if (compilation.directives.exists()) compilation.directives.readText() else ""

        val messageCollector = LocationReportingTestMessageCollector()
        val compiler = K2JVMCompiler()
        val args = compiler.createArguments().apply {
            freeArgs = listOf(compilation.srcDir.canonicalPath)
            classpath = (abiDependencies + kotlinJvmStdlib).joinToString(File.pathSeparator) { it.canonicalPath }
            pluginClasspaths = arrayOf(abiPluginJar.canonicalPath)
            pluginOptions = listOfNotNull(
                abiOption(JvmAbiCommandLineProcessor.OUTPUT_PATH_OPTION.optionName, compilation.abiDir.canonicalPath),
                if (useLegacyAbiGen) abiOption("useLegacyAbiGen", "true") else null
            ).toTypedArray()
            destination = compilation.destinationDir.canonicalPath
            noSourceDebugExtension = InTextDirectivesUtils.findStringWithPrefixes(directives, "// NO_SOURCE_DEBUG_EXTENSION") != null

            if (InTextDirectivesUtils.findStringWithPrefixes(directives, "// USE_K2") != null) {
                useK2 = true

                // Force metadata version 1.9 to circumvent the fact that kotlinx-metadata-jvm 0.6.0 has default metadata version 1.8,
                // so it can read/write metadata with versions up to and including 1.9, yet K2 has metadata version 2.0+.
                // This hack can be removed once jvm-abi-gen depends on kotlinx-metadata-jvm that can read/write metadata version 2.0.
                // Without this hack, CompileAgainstJvmAbiTestGenerated.testInlineClassWithPrivateConstructorK2 currently fails.
                if (KotlinClassMetadata.COMPATIBLE_METADATA_VERSION.take(2) == listOf(1, 8)) {
                    metadataVersion = "1.9"
                }
            }
        }
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)
        if (exitCode != ExitCode.OK || messageCollector.errors.isNotEmpty()) {
            val errorLines = listOf("Could not compile $compilation", "Exit code: $exitCode", "Errors:") + messageCollector.errors
            error(errorLines.joinToString("\n"))
        }

        // Compile Java files into both the destination and ABI directories in order make the
        // results available to run and to downstream compilations.
        val javaFiles = CodegenTestUtil.findJavaSourcesInDirectory(compilation.srcDir).map(::File)
        if (javaFiles.isNotEmpty()) {
            compilation.javaDestinationDir.mkdirs()
            val javacOptions = listOf(
                "-classpath",
                (abiDependencies + compilation.destinationDir).joinToString(File.pathSeparator) { it.canonicalPath }
                        + File.pathSeparator + ForTestCompileRuntime.runtimeJarForTests(),
                "-d",
                compilation.javaDestinationDir.canonicalPath
            )
            KotlinTestUtils.compileJavaFiles(javaFiles, javacOptions)
            FileUtil.copyDir(compilation.javaDestinationDir, compilation.destinationDir)
            FileUtil.copyDir(compilation.javaDestinationDir, compilation.abiDir)
        }
    }

    protected open val useLegacyAbiGen: Boolean
        get() = false

    protected val kotlinJvmStdlib = File("dist/kotlinc/lib/kotlin-stdlib.jar").also {
        check(it.exists()) { "Stdlib file '$it' does not exist" }
    }
}
