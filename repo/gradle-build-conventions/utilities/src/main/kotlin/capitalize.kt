import java.util.Locale

fun String.capitalize(): String = capitalize(Locale.ROOT)

fun String.capitalize(locale: Locale): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
