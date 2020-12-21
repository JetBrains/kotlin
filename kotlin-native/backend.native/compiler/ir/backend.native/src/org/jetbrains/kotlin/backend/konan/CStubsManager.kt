package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.KonanTarget

private const val dumpBridges = false

class CStubsManager(private val target: KonanTarget) {

    fun getUniqueName(prefix: String) = "$prefix${counter++}"

    fun addStub(kotlinLocation: CompilerMessageLocation?, lines: List<String>, language: String) {
        val stubs = languageToStubs.getOrPut(language) { mutableListOf() }
        stubs += Stub(kotlinLocation, lines)
    }

    fun compile(clang: ClangArgs, messageCollector: MessageCollector, verbose: Boolean): List<File> {
        if (languageToStubs.isEmpty()) return emptyList()

        val bitcodes = languageToStubs.entries.map { (language, stubs) ->
            val compilerOptions = mutableListOf<String>()
            val sourceFileExtension = when {
                language == "C++" -> ".cpp"
                target.family.isAppleFamily -> {
                    compilerOptions += "-fobjc-arc"
                    ".m" // TODO: consider managing C and Objective-C stubs separately.
                }
                else -> ".c"
            }
            val cSource = createTempFile("cstubs", sourceFileExtension).deleteOnExit()
            cSource.writeLines(stubs.flatMap { it.lines })

            val bitcode = createTempFile("cstubs", ".bc").deleteOnExit()

            val cSourcePath = cSource.absolutePath

            val clangCommand = clang.clangC(
                *compilerOptions.toTypedArray(), "-O2",
                cSourcePath, "-emit-llvm", "-c", "-o", bitcode.absolutePath
            )
            if (dumpBridges) {
                println("CSTUBS for ${language}")
                stubs.flatMap { it.lines }.forEach {
                    println(it)
                }
                println("CSTUBS in ${cSource.absolutePath}")
                println("CSTUBS CLANG COMMAND:")
                println(clangCommand.joinToString(" "))
            }

            val result = Command(clangCommand).getResult(withErrors = true)
            if (result.exitCode != 0) {
                reportCompilationErrors(cSourcePath, stubs, result, messageCollector, verbose)
            }
            bitcode
        }

        return bitcodes
    }

    private fun reportCompilationErrors(
            cSourcePath: String,
            stubs: List<Stub>,
            result: Command.Result,
            messageCollector: MessageCollector,
            verbose: Boolean
    ): Nothing {
        val regex = Regex("${Regex.escape(cSourcePath)}:([0-9]+):[0-9]+: error: .*")
        val errorLines = result.outputLines.mapNotNull { line ->
            regex.matchEntire(line)?.let { matchResult ->
                matchResult.groupValues[1].toInt()
            }
        }

        val lineToStub = ArrayList<Stub>()
        stubs.forEach { stub ->
            repeat(stub.lines.size) { lineToStub.add(stub) }
        }

        val cSourceCopyPath = "cstubs.c"
        if (verbose) {
            File(cSourcePath).copyTo(File(cSourceCopyPath))
        }

        if (errorLines.isNotEmpty()) {
            errorLines.forEach {
                messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Unable to compile C bridge" + if (verbose) " at $cSourceCopyPath:$it" else "",
                        lineToStub[it - 1].kotlinLocation
                )
            }
        } else {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unable to compile C bridges",
                    null
            )
        }

        throw KonanCompilationException()
    }

    private val languageToStubs = mutableMapOf<String, MutableList<Stub>>()
    private class Stub(val kotlinLocation: CompilerMessageLocation?, val lines: List<String>)
    private var counter = 0
}