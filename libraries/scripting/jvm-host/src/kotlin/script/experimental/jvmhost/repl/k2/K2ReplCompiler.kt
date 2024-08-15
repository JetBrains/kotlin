/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.script.experimental.api.ReplCompiler
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.jvm.util.KotlinJars.k2ReplTestsClassPath
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

/**
 * Dummy class only used for experimentation, while the final API for K2 Repl is being
 * developed.
 */
class K2ReplCompiler(private val outputDir: File) : ReplCompiler<K2CompiledSnippet> {
    override val lastCompiledSnippet: LinkedSnippet<K2CompiledSnippet>?
        get() = TODO("Not yet implemented")

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<K2CompiledSnippet>> {
        var compiledSnippets: LinkedSnippetImpl<K2CompiledSnippet>? = null
        snippets.forEachIndexed { i, snippet ->
            val sourceFile = File(outputDir, snippet.name!!)
            sourceFile.createNewFile() // If we do not create the file first, `writeText` does nothing
            sourceFile.writeText(snippet.text)
            val outStream = ByteArrayOutputStream()
            val compileExitCode = K2JVMCompiler().exec(
                PrintStream(outStream),
                "-d", outputDir.absolutePath + "/out",
                "-no-stdlib",
                "-cp", k2ReplTestsClassPath.joinToString(File.pathSeparator),
                "-language-version", "2.0",
                sourceFile.absolutePath
            )
            if (compileExitCode.code != 0) {
                error("Compilation failed: $compileExitCode: $outStream")
            }

            // Just hardcode the FQN for the snippet for now
            // Make sure this matches the one in `ReplSmokeTests`
            val packageName = "repl.snippet$i.Snippet$i"
            val buildDir = File(outputDir, "out/repl/snippet$i")
            compiledSnippets = compiledSnippets.add(K2CompiledSnippet(buildDir, packageName))
        }
        return compiledSnippets!!.asSuccess()
    }
}
