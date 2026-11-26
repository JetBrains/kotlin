import java.io.File

fun main() {
    for (run in listOf("jtree", "direct", "headerMode", "noValhalla")) {
        println("$run:")
        println(File("output/$run/sources/report.txt").readText())
        println()
    }
    // The metadata arguments change with the compiler version, so they are removed
    val metadata = "@kotlin\\.Metadata\\(.*\\)".toRegex()
    for (run in listOf("jtree", "direct", "headerMode")) {
        println("$run stub:")
        println(File("output/$run/stubs/Money.java").readText().replace(metadata, "@kotlin.Metadata()").trim())
        println()
    }
}
