import kotlin.modules.*

fun project() {
    module("testlib") {
        classpath += "lib/junit-4.9.jar"

        addSourceFiles("test")
    }
}