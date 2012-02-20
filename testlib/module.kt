import kotlin.modules.*

fun project() {
    module("testlib") {
        // TODO how to refer to the dir of the module?
        classpath += "testlib/lib/junit-4.9.jar"

        addSourceFiles("test")
    }
}