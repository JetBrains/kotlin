package injection

import org.intellij.lang.annotations.Language

fun Int.html(@org.intellij.lang.annotations.Language("HTML") html: String) {}

fun Int.regexp(@Language("RegExp") html: String) {}

