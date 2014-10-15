import kotlin.modules.*

fun project() {
    module("apidocs") {
        classpath += "../../lib/junit-4.11.jar"

        addSourceFiles("../../stdlib/src")
        addSourceFiles("../../kunit/src/main/kotlin")
        addSourceFiles("../../kotlin-jdbc/src/main/kotlin")
        //addSourceFiles("../kotlin-swing/src/main/kotlin")
    }
}