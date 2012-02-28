import kotlin.modules.*

fun project() {
    module("testlib") {
        // TODO how to refer to the dir of the module?
        classpath += "dist/kotlinc/lib/kotlin-test.jar"
        classpath += "kunit/lib/junit-4.9.jar"

        addSourceFiles("test")
    }
}