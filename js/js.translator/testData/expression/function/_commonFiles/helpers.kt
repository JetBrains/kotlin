package foo

native
fun String.replace(regexp: RegExp, replacement: String): String = noImpl

fun String.replaceAll(regexp: String, replacement: String): String = replace(RegExp(regexp, "g"), replacement)

native
fun String.search(regexp: RegExp): Int = noImpl

native
class RegExp(regexp: String, flags: String = "") {
    fun exec(s: String): Array<String>? = noImpl
}