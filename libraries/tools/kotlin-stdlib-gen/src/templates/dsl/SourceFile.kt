package templates

enum class SourceFile(jvmClassName: String? = null, val multifile: Boolean = true, val packageName: String? = null) {

    Arrays(packageName = "kotlin.collections"),
    Collections(packageName = "kotlin.collections"),
    Sets(packageName = "kotlin.collections"),
    Maps(packageName = "kotlin.collections"),
    Sequences(packageName = "kotlin.sequences"),
    Ranges(packageName = "kotlin.ranges"),
    Comparisons(packageName = "kotlin.comparisons"),
    Strings(packageName = "kotlin.text"),
    Misc(),
    ;

    val jvmClassName = jvmClassName ?: (name.capitalize() + "Kt")
}
