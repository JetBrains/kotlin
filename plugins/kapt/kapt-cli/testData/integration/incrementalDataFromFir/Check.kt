import java.io.File

fun main() {
    require(File("output/incremental-data/test/TestKt.class").isFile) {
        "No file facade was generated in the incremental data"
    }

    // Incremental compilation finds the top-level declarations of unchanged files through the module mapping.
    require(File("output/incremental-data/META-INF/main.kotlin_module").isFile) {
        "No module mapping was generated in the incremental data"
    }
}
