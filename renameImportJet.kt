import java.io.File
import com.intellij.util.Processor
import com.intellij.openapi.util.io.FileUtil
import java.util.ArrayList

fun main(args: Array<String>) {
    println("renameImportJet")

    fun processFile(file: File) {
        if (!file.getName().endsWith(".java") || file.isDirectory()) return

        val lines = ArrayList<String>()
        var rewrite = false
        file.reader().forEachLine { line ->
            if (line.startsWith("import jet.") && !line.startsWith("import jet.runtime.typeinfo"))  {
                lines.add("import kotlin." + line.substring("import jet.".length))
                rewrite = true
            }
            else {
                lines.add(line)
            }
        }

        if (rewrite) {
            println("renaming imports in $file")
            file.writeText(lines reduce { (a, b) -> "$a\n$b" })
        }
    }

    val processor = Processor<File> { if (it != null) processFile(it); true }
    val directoryFilter = Processor<File> { it?.getName() != "testData" }

    FileUtil.processFilesRecursively(File("."), processor, directoryFilter)
}
