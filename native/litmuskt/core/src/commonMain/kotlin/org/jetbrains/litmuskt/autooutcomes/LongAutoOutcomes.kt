package org.jetbrains.litmuskt.autooutcomes

import org.jetbrains.litmuskt.LitmusOutcomeSpecScope

open class LitmusLOutcome(
    var r1: Long = 0,
) : LitmusAutoOutcome {
    final override fun toString() = "$r1"
    final override fun hashCode() = r1.toInt()
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusLOutcome) return false
        return r1 == o.r1
    }

    final override fun toList() = listOf(r1)
    final override fun parseOutcome(str: String): LitmusLOutcome {
        val rs = str.split(", ").map(String::toLong)
        return LitmusLOutcome(rs[0])
    }
}

fun <S : LitmusLOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Long) =
    accept(setOf(LitmusLOutcome(r1)))

fun <S : LitmusLOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Long) =
    interesting(setOf(LitmusLOutcome(r1)))

fun <S : LitmusLOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Long) =
    forbid(setOf(LitmusLOutcome(r1)))

open class LitmusLLOutcome(
    var r1: Long = 0,
    var r2: Long = 0
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2)"
    final override fun hashCode() = ((r1 shl 16) + r2).toInt()
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusLLOutcome) return false
        return r1 == o.r1 && r2 == o.r2
    }

    final override fun toList() = listOf(r1, r2)
    final override fun parseOutcome(str: String): LitmusLLOutcome {
        val rs = str.split(", ").map(String::toLong)
        return LitmusLLOutcome(rs[0], rs[1])
    }
}

fun <S : LitmusLLOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Long, r2: Long) =
    accept(setOf(LitmusLLOutcome(r1, r2)))

fun <S : LitmusLLOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Long, r2: Long) =
    interesting(setOf(LitmusLLOutcome(r1, r2)))

fun <S : LitmusLLOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Long, r2: Long) =
    forbid(setOf(LitmusLLOutcome(r1, r2)))

open class LitmusLLLOutcome(
    var r1: Long = 0,
    var r2: Long = 0,
    var r3: Long = 0,
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2, $r3)"
    final override fun hashCode() = ((r1 shl 20) + (r2 shl 10) + r3).toInt()
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusLLLOutcome) return false
        return r1 == o.r1 && r2 == o.r2 && r3 == o.r3
    }

    final override fun toList() = listOf(r1, r2, r3)
    final override fun parseOutcome(str: String): LitmusLLLOutcome {
        val rs = str.split(", ").map(String::toLong)
        return LitmusLLLOutcome(rs[0], rs[1], rs[2])
    }
}

fun <S : LitmusLLLOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Long, r2: Long, r3: Long) =
    accept(setOf(LitmusLLLOutcome(r1, r2, r3)))

fun <S : LitmusLLLOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Long, r2: Long, r3: Long) =
    interesting(setOf(LitmusLLLOutcome(r1, r2, r3)))

fun <S : LitmusLLLOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Long, r2: Long, r3: Long) =
    forbid(setOf(LitmusLLLOutcome(r1, r2, r3)))

open class LitmusLLLLOutcome(
    var r1: Long = 0,
    var r2: Long = 0,
    var r3: Long = 0,
    var r4: Long = 0,
) : LitmusAutoOutcome {
    final override fun toString() = "($r1, $r2, $r3, $r4)"
    final override fun hashCode() = ((r1 shl 24) + (r2 shl 16) + (r3 shl 8) + r4).toInt()
    final override fun equals(o: Any?): Boolean {
        if (o !is LitmusLLLLOutcome) return false
        return r1 == o.r1 && r2 == o.r2 && r3 == o.r3 && r4 == o.r4
    }

    final override fun toList() = listOf(r1, r2, r3, r4)
    final override fun parseOutcome(str: String): LitmusLLLLOutcome {
        val rs = str.split(", ").map(String::toLong)
        return LitmusLLLLOutcome(rs[0], rs[1], rs[2], rs[3])
    }
}

fun <S : LitmusLLLLOutcome> LitmusOutcomeSpecScope<S>.accept(r1: Long, r2: Long, r3: Long, r4: Long) =
    accept(setOf(LitmusLLLLOutcome(r1, r2, r3, r4)))

fun <S : LitmusLLLLOutcome> LitmusOutcomeSpecScope<S>.interesting(r1: Long, r2: Long, r3: Long, r4: Long) =
    interesting(setOf(LitmusLLLLOutcome(r1, r2, r3, r4)))

fun <S : LitmusLLLLOutcome> LitmusOutcomeSpecScope<S>.forbid(r1: Long, r2: Long, r3: Long, r4: Long) =
    forbid(setOf(LitmusLLLLOutcome(r1, r2, r3, r4)))
