package test

import java.io.File

@Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno

val annotatedType: String = @Anno "something"

fun main() {
    val output = File("plugin-generated-file").readText()
    if (output == "frontend plugin applied\nbackend plugin applied") {
        println("OK")
    } else {
        println("Fail: both frontend and backend plugins should be applied. Output: $output")
    }
}
