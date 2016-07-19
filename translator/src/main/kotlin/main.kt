import com.intellij.openapi.util.Disposer
import org.kotlinnative.translator.FileTranslator
import org.kotlinnative.translator.parseAndAnalyze

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Enter filename")
        return
    }

    val disposer = Disposer.newDisposable()
    val state = parseAndAnalyze(args.asList(), disposer, arm = true)

    val files = state.environment.getSourceFiles()
    if (files.isEmpty()) {
        print("Empty")
        return
    }

    println(FileTranslator(state, files[0]).generateCode())
}

