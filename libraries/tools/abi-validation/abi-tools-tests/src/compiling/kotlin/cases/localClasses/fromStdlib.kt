package cases.localClasses

import kotlin.comparisons.compareBy

private val COMPARER = compareBy<String> { it.length }