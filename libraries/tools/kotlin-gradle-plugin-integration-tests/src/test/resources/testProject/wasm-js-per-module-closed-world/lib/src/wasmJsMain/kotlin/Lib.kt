class Lib {
    operator fun invoke(block: SceneBuilder.() -> Unit): String =
        SceneBuilder().apply(block).build()
}

class SceneBuilder {
    private val objects = mutableListOf<String>()

    operator fun String.unaryPlus() {
        objects += this
    }

    fun build(): String = "Scene: [${objects.joinToString(", ")}]"
}
