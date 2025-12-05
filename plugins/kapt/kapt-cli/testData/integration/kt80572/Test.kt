import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

// Print only methods
fun main() {
    val path: Path = Paths.get("output/stubs/FooAnnotationUser.java")
    val lines: List<String> = Files.readAllLines(path, StandardCharsets.UTF_8)

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()

        if (line.startsWith("@") && !line.startsWith("@kotlin.Metadata")) {
            println(lines[i])

            var j = i + 1
            while (j < lines.size && lines[j].isBlank()) {
                j++
            }
            if (j < lines.size) {
                val nextLine = lines[j].trim()
                if (nextLine.endsWith(");")) {
                    println(lines[j])
                    println()
                }
            }
            i = j
        } else {
            i++
        }
    }
}