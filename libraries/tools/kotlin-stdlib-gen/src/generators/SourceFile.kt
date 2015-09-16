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

    val name = this.name()

    val jvmClassName = jvmClassName ?: this.name().capitalize()

    val fileName: String get() = "_${name.capitalize()}.kt"
}
