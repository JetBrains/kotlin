import java.io.File

fun main() {
    require(File("output/stubs").walkTopDown().any { it.isFile }) {
        "No stub files were generated"
    }

    require(
        File("output/incremental-data")
            .walkTopDown()
            .any { it.isFile && it.extension == "class" }
    ) {
        "No incremental-data class files were generated"
    }
}
