package test

public fun unchangedFun() {}

public fun removedFun(): Int = 10

public val removedVal: String = "A"

public val changedVal: Int = 20

internal fun changedFun(arg: Int) {}

private fun privateRemovedFun() {}