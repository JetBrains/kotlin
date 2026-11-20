import java.io.File

fun main() {
    val first = File("output/stats-first.txt").readText()
    val second = File("output/stats-second.txt").readText()
    val third = File("output/stats-third.txt").readText()

    check("total sources: 3" in first) {
        "Expected three generated sources in the first run:\n$first"
    }

    check("total sources: 2" in second) {
        "Expected the two sources depending on lib.A and lib.B in the incremental run:\n$second"
    }

    check("total sources: 2" in third) {
        "Expected comma-separated classpath changes to be split:\n$third"
    }

    println("OK")
}
