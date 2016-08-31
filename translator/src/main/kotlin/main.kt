import com.intellij.openapi.util.Disposer
import com.jshmrsn.karg.parseArguments
import org.kotlinnative.translator.ProjectTranslator
import org.kotlinnative.translator.parseAndAnalyze
import java.io.*

fun main(args: Array<String>) {
    val arguments = parseArguments(args, ::DefaultArguments)
    val disposer = Disposer.newDisposable()
    val analyzedFiles = arguments.sources.toMutableList()

    if (arguments.includeDir != null) {
        val libraryFiles = File(arguments.includeDir).walk().filter { !it.isDirectory }.map { it.absolutePath }
        analyzedFiles.addAll(libraryFiles)
    }

    val state = parseAndAnalyze(analyzedFiles, disposer, arguments.mainClass, arguments.arm)
    val files = state.environment.getSourceFiles()
    val code = ProjectTranslator(files, state).generateCode()

    if (arguments.output == null) {
        println(code)
    } else {
        val output = File(arguments.output)
        output.writeText(code)
    }
}