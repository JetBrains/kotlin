import kotlin.modules.*

fun project() {
    module("kdoc") {
        classpath += "dist/kotlinc/lib/intellij-core.jar"
        classpath += "dist/kotlinc/lib/kotlin-compiler.jar"
        addSourceFiles("src/main/kotlin")
    }
}