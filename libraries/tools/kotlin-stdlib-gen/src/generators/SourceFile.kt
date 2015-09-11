package generators

enum class SourceFile(jvmClassName: String? = null) {

    Arrays(),
    Collections(),
    Sets(),
    Maps(),
    Sequences(),
    Ranges(),
    Strings(),
    Numbers(),
    ;

    val name = this.name()

    val jvmClassName = jvmClassName ?: this.name().capitalize()

    val fileName: String get() = "_${name.capitalize()}.kt"
}
