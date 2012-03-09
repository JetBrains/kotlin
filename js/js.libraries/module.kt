import kotlin.modules.*

fun project() {
    module("apidocs") {
        addSourceFiles("src/core")
        addSourceFiles("src/html5")
        addSourceFiles("src/jquery")
        addSourceFiles("src/raphael")
    }
}