package test

import java.io.File

fun main() {
    val output = File("plugin-generated-file").readText()
    if (output == "frontend plugin applied") {
        println("OK")
    } else {
        println("Fail: frontend plugin is not applied. Output: $output")
    }
}
