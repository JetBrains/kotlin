import java.io.File

fun main() {
    check(File("output/sources/test/FirstGenerated.java").isFile)
    check(File("output/sources/test/SecondGenerated.java").isFile)

    val first = File("output/stats-first.txt").readText()
    val second = File("output/stats-second.txt").readText()

    check("total sources: 2" in first) {
        "Expected two generated sources in the first run:\n$first"
    }

    check("total sources: 1" in second) {
        "Expected one generated source in the incremental run:\n$second"
    }

    println("OK")
}
