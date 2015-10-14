package generators

enum class SourceFile(jvmClassName: String? = null, val multifile: Boolean = true) {

    Arrays(),
    Collections(),
    Sets(),
    Maps(),
    Sequences(),
    Ranges(),
    Strings(),
    Misc(),
    ;

    val jvmClassName = jvmClassName ?: (name.capitalize() + "Kt")

    val fileName: String get() = "_${name.capitalize()}.kt"
}
