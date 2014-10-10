import kotlin.modules.*

fun project() {
    module("testlib") {
        classpath += "lib/junit-4.11.jar"

        addSourceFiles("test")
    }
}