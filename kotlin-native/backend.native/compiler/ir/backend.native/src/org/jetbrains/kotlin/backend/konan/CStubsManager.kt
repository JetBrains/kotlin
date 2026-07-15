package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.io.deleteOnExit
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createTempFile
import kotlin.io.path.writeLines

private const val dumpBridges = false

internal class CStubsManager(private val target: KonanTarget, private val generationState: NativeGenerationState) {

    fun addStub(kotlinLocation: CompilerMessageLocation?, lines: List<String>, language: String) {
        val stubs = languageToStubs.getOrPut(language) { mutableListOf() }
        stubs += Stub(kotlinLocation, lines)
    }

    fun compile(clang: ClangArgs, diagnosticReporter: IrDiagnosticReporter, verbose: Boolean): List<Path> {
        if (languageToStubs.isEmpty()) return emptyList()

        val bitcodes = languageToStubs.entries.map { [language, stubs] ->
            val compilerOptions = mutableListOf<String>()
            val sourceFileExtension = when {
                language == "C++" -> ".cpp"
                target.family.isAppleFamily -> {
                    compilerOptions += "-fobjc-arc"
                    ".m" // TODO: consider managing C and Objective-C stubs separately.
                }
                else -> ".c"
            }
            val cSource = createTempFile("cstubs", sourceFileExtension).apply { deleteOnExit() }
            cSource.writeLines(stubs.flatMap { it.lines })

            val bitcode = createTempFile("cstubs", ".bc").apply { deleteOnExit() }

            val cSourcePath = cSource.absolutePathString()

            val clangCommand = clang.clangC(
                    *compilerOptions.toTypedArray(), "-O2",
                    "-fexceptions", // Allow throwing exceptions through generated stubs.
                    cSourcePath, "-emit-llvm", "-c", "-o", bitcode.absolutePathString()
            )
            if (dumpBridges) {
                println("CSTUBS for ${language}")
                stubs.flatMap { it.lines }.forEach {
                    println(it)
                }
                println("CSTUBS in ${cSource.absolutePathString()}")
                println("CSTUBS CLANG COMMAND:")
                println(clangCommand.joinToString(" "))
            }

            val result = Command(clangCommand).getResult(withErrors = true)
            if (result.exitCode != 0) {
                reportCompilationErrors(cSourcePath, stubs, result, diagnosticReporter, verbose)
            }
            bitcode
        }

        return bitcodes
    }

    private fun reportCompilationErrors(
            cSourcePath: String,
            stubs: List<Stub>,
            result: Command.Result,
            diagnosticReporter: IrDiagnosticReporter,
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
            Path(cSourcePath).copyTo(Path(cSourceCopyPath), overwrite = true)
        }

        if (errorLines.isNotEmpty()) {
            errorLines.forEach {
                diagnosticReporter.report(
                        NativeBackendDiagnostics.NATIVE_BACKEND_ERROR,
                        "Unable to compile C bridge" + if (verbose) " at $cSourceCopyPath:$it" else "",
                        lineToStub[it - 1].kotlinLocation
                )
            }
        } else {
            diagnosticReporter.report(
                    NativeBackendDiagnostics.NATIVE_BACKEND_ERROR,
                    "Unable to compile C bridges",
                    null
            )
        }

        throw KonanCompilationException()
    }

    private val languageToStubs = mutableMapOf<String, MutableList<Stub>>()
    private class Stub(val kotlinLocation: CompilerMessageLocation?, val lines: List<String>)
}