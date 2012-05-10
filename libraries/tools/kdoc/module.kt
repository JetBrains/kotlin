import kotlin.modules.*

fun project() {
    module("kdoc") {
        classpath += "dist/kotlinc/lib/intellij-core.jar"
        classpath += "dist/kotlinc/lib/kotlin-compiler.jar"

        // TODO its a bit sad we can't use patterns here...
        addClasspathEntry("dist/kotlinc/lib/pegdown-1.1.0.jar")
        addClasspathEntry("dist/kotlinc/lib/parboiled-core-1.0.2.jar")
        addClasspathEntry("dist/kotlinc/lib/parboiled-java-1.0.2.jar")

        addSourceFiles("src/main/kotlin")
    }
}