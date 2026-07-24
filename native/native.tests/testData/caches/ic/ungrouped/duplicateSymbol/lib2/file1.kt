package app

internal fun Data.tagged(): Int = n + 1

fun bValue(d: Data): Int = d.tagged()
