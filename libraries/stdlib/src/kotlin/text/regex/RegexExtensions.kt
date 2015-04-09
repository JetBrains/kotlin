package kotlin

import kotlin.text.*

public fun String.toRegex(vararg options: RegexOption): Regex = Regex(this, *options)
public fun String.toRegex(options: Set<RegexOption>): Regex = Regex(this, options)
