package foo

inline fun String.replace(regexp: RegExp, replacement: String): String = asDynamic().replace(regexp, replacement)

fun String.replaceAll(regexp: String, replacement: String): String = replace(RegExp(regexp, "g"), replacement)

inline fun String.search(regexp: RegExp): Int = asDynamic().search(regexp)

external class RegExp(regexp: String, flags: String = definedExternally) {
    fun exec(s: String): Array<String>? = definedExternally
}