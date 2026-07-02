package app

internal fun Data.tagged(): Int = n + 1

fun aValue(d: Data): Int = d.tagged() + 1
