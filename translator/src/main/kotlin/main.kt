import com.intellij.openapi.util.Disposer
import com.jshmrsn.karg.parseArguments
import org.kotlinnative.translator.ProjectTranslator
import org.kotlinnative.translator.parseAndAnalyze
import java.io.File
import java.util.*

fun main(args: Array<String>) {
    val arguments = parseArguments(args, ::DefaultArguments)
    val disposer = Disposer.newDisposable()
    val analyzedFiles = ArrayList<String>()

    val stdlib = mutableListOf<String>()
    if (arguments.includeDir != null) {
        stdlib.addAll(File(arguments.includeDir).listFiles().map { it.absolutePath })
        analyzedFiles.addAll(stdlib)
    }

    analyzedFiles.addAll(arguments.sources)

    val state = parseAndAnalyze(analyzedFiles, disposer, arguments.arm ?: false)
    val files = state.environment.getSourceFiles()
    val code = ProjectTranslator(files, state).generateCode()

    if (arguments.output == null) {
        println(code)
        return
    }

    val output = File(arguments.output)
    output.writeText(code)
}

