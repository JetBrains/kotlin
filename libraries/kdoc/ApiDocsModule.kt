import kotlin.modules.*

fun project() {
    module("apidocs") {
        classpath += "../lib/junit-4.9.jar"

        addSourceFiles("../stdlib/src")
        addSourceFiles("../kunit/src/main/kotlin")
        addSourceFiles("../kotlin-jdbc/src/main/kotlin")
    }
}