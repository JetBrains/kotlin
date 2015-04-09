package library.sample

import kotlin.js.Date

public fun pairAdd(p: Pair<Int, Int>): Int = p.first + p.second

public fun pairMul(p: Pair<Int, Int>): Int = p.first * p.second

public data class IntHolder(val value: Int)

public fun Date.extFun(): Int = 1000