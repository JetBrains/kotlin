import java.io.File

fun main() {
    for (run in listOf("jtree", "direct", "noValhalla")) {
        println("$run:")
        println(File("output/$run/sources/report.txt").readText())
        println()
    }
}
