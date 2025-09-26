package hair.ir.generator.toolbox

import java.util.*

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
fun String.decapitalize() = this.replaceFirstChar { it.lowercase(Locale.getDefault()) }
