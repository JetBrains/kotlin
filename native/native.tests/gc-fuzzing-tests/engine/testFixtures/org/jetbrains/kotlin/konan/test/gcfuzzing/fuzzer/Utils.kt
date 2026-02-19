/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.fuzzer

import kotlin.math.roundToInt
import kotlin.random.Random

interface Distribution<T> {
    fun next(random: Random): T

    companion object {
        fun <T> uniform(vararg discreteValues: T): Distribution<T> = object : Distribution<T> {
            override fun next(random: Random): T = discreteValues.random(random)
        }

        inline fun <reified T : Enum<T>> uniform(): Distribution<T> = uniform(*enumValues<T>())

        inline fun <reified T> weighted(vararg variants: Pair<T, Int>): Distribution<T> =
            uniform(*(variants.flatMap { (variant, weight) ->
                List(weight) { variant }
            }.toTypedArray()))

        fun uniformalBetween(from: Int, until: Int): Distribution<Int> = CumulativeFun.uniform(
            0.0 to from,
            1.0 to until
        )
    }
}

fun interface CumulativeFun<T> : Distribution<T> {
    fun quantile(p: Double): T

    override fun next(random: Random): T {
        val p = random.nextDouble(0.0, 1.0)
        return quantile(p)
    }

    companion object {
        fun uniform(vararg anchors: Pair<Double, Int>): CumulativeFun<Int> = object : CumulativeFun<Int> {
            override fun quantile(p: Double): Int {
                val sortedAnchors = anchors.sortedBy { it.first }
                val greaterIndex = sortedAnchors.indexOfFirst { it.first > p }
                val from = sortedAnchors[greaterIndex - 1]
                val to = sortedAnchors[greaterIndex]
                val valDiff = to.second - from.second
                val pDiff = to.first - from.first
                val wantedPDiff = p - from.first
                return (from.second + wantedPDiff * valDiff / pDiff).roundToInt()
            }
        }
    }
}
