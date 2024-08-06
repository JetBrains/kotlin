package org.jetbrains.litmuskt.autooutcomes

import org.jetbrains.litmuskt.LitmusOutcomeSpecScope

/**
 * "Z" is the name for Boolean outcomes in JCStress.
 */

// TODO: codegen

open class LitmusZZOutcome(
    var r1: Boolean = false,
    var r2: Boolean = false
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2)"
    final override fun hashCode() = (if (r1) 1 else 0) + (if (r2) 2 else 3)
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusZZOutcome) return false
        return r1 == o.r1 && r2 == o.r2
    }

    final override fun toList() = listOf(r1, r2)
    final override fun parseOutcome(str: String): LitmusZZOutcome {
        val rs = str.split(", ").map(String::toBooleanStrict)
        return LitmusZZOutcome(rs[0], rs[1])
    }
}

fun <S : LitmusZZOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Boolean, r2: Boolean) =
    accept(setOf(LitmusZZOutcome(r1, r2)))

fun <S : LitmusZZOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Boolean, r2: Boolean) =
    interesting(setOf(LitmusZZOutcome(r1, r2)))

fun <S : LitmusZZOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Boolean, r2: Boolean) =
    forbid(setOf(LitmusZZOutcome(r1, r2)))
