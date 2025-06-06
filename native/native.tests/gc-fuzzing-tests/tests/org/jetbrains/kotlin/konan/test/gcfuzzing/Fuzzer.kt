/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing

import kotlin.random.*
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.*
import kotlin.math.roundToInt

interface Distribution<T> {
    fun next(random: Random): T

    companion object {
        fun <T> uniformal(vararg discreteValues: T): Distribution<T> = object : Distribution<T> {
            override fun next(random: Random): T {
                val idx = random.nextInt(0, discreteValues.size)
                return discreteValues[idx]
            }
        }
        inline fun <reified T: Enum<T>> uniformal(): Distribution<T> = uniformal(*enumValues<T>())

        inline fun <reified T> weighted(vararg variants: Pair<T, Int>): Distribution<T> = uniformal(*(variants.map { it.first }.toTypedArray()))

        fun uniformalBetween(from: Int, until: Int): Distribution<Int> = CumulativeFun.uniformal(
            0.0 to from,
            1.0 to until
        )
    }
}

fun interface CumulativeFun<T> : Distribution<T> {
    fun cdf(p: Double): T

    override fun next(random: Random): T {
        val p = random.nextDouble(0.0, 1.0)
        return cdf(p)
    }

    companion object {
        fun uniformal(vararg anchors: Pair<Double, Int>): CumulativeFun<Int> = object : CumulativeFun<Int> {
            override fun cdf(p: Double): Int {
                val sortedAnchors = anchors.sortedBy { it.first }
                val greaterIndex = sortedAnchors.indexOfFirst { it.first > p }
                val from = sortedAnchors[greaterIndex - 1].second
                val to = sortedAnchors[greaterIndex].second
                return (from + p * (to - from)).roundToInt()
            }
        }
    }
}

enum class DefinitionKind {
    CLASS, FUNCTION, GLOBAL
}

enum class StatementKind {
    ALLOC, LOAD, STORE, CALL, SPAWN_THREAD
}

enum class AccessKind {
    LOCAL, GLOBAL
}

class Fuzzer(seed: Int = 0, val distributions: Distributions = Distributions()) {
    val random = Random(seed)

    // TODO first generate all symbols only then bodies

    class Distributions {
        val numDefinitions = Distribution.uniformalBetween(0, 20) // FIXME make normal?
        val language = Distribution.uniformal(TargetLanguage.Kotlin) // TODO other languages
        val definition = Distribution.uniformal<DefinitionKind>()
        val numFields = CumulativeFun.uniformal(
            0.0 to 0,
            0.1 to 0,
            0.5 to 2,
            0.9 to 10,
            0.999 to 1000,
            1.0 to Int.MAX_VALUE
        )
        val numParameters = CumulativeFun.uniformal(
            0.0 to 0,
            0.9 to 10,
            1.0 to 256
        )
        // FIXME completely arbitrary
        val bodySize = Distribution.uniformalBetween(0, 100)
        val statement = Distribution.weighted(
            StatementKind.ALLOC to 1,
            StatementKind.LOAD to 1,
            StatementKind.STORE to 1,
            StatementKind.CALL to 1,
            StatementKind.SPAWN_THREAD to 1,
        )
        val pathLength = CumulativeFun.uniformal(
            0.0 to 0,
            0.1 to 0,
            0.9 to 3,
            1.0 to 100
        )
        val access = Distribution.uniformal<AccessKind>() // FIXME should be more locals?
    }

    private val numDefinitions: Map<DefinitionKind, Int> = (0 until distributions.numDefinitions.next(random)).map {
        distributions.definition.next(random)
    }.groupBy{ it }.mapValues { it.value.size }

    private fun genDefinitions(): List<Definition> {
        val globals = List(numDefinitions[DefinitionKind.GLOBAL]!!) { genGlobal() }
        val classes = List(numDefinitions[DefinitionKind.CLASS]!!) { genClass() }
        val functions = List(numDefinitions[DefinitionKind.FUNCTION]!!) { genFunction() }
        return globals + classes + functions
    }

    fun genProgram(): Program = Program(
        definitions =  genDefinitions(),
        mainBody = genBody()
    )

    fun genClass(): Definition.Class {
        val lang = distributions.language.next(random)
        val numFields = distributions.numFields.next(random)
        return Definition.Class(
            lang,
            List(numFields) { Field }
        )
    }

    fun genFunction(): Definition.Function {
        val lang = distributions.language.next(random)
        val numParameters = distributions.numParameters.next(random)
        return Definition.Function(
            lang,
            List(numParameters) { Parameter },
            genBodyWithReturn(),
        )
    }

    fun genGlobal(): Definition.Global {
        val lang = distributions.language.next(random)
        return Definition.Global(lang, Field)
    }

    fun genBodyWithReturn() = BodyWithReturn(genBody(), LoadExpression.Default) // FIXME non-default load?

    fun genBody(): Body {
        return Body(List(distributions.bodySize.next(random)) { genStatement() })
    }

    fun genStatement(): BodyStatement = when (distributions.statement.next(random)) {
        StatementKind.ALLOC -> genAlloc()
        StatementKind.LOAD -> genLoad()
        StatementKind.STORE -> genStore()
        StatementKind.CALL -> genCall()
        StatementKind.SPAWN_THREAD -> genSpawnThread()
    }

    private fun chooseClass(): Int = random.nextInt(0, numDefinitions[DefinitionKind.CLASS]!!)
    private fun chooseFunction(): Int = random.nextInt(0, numDefinitions[DefinitionKind.FUNCTION]!!)
    private fun chooseGlobal(): Int = random.nextInt(0, numDefinitions[DefinitionKind.GLOBAL]!!)
    private fun chooseLocal(): Int = random.nextInt(0, Int.MAX_VALUE)

    fun genAlloc(): BodyStatement.Alloc {
        return BodyStatement.Alloc(chooseClass(), genArgs())
    }

    fun genLoad(): BodyStatement.Load {
        return BodyStatement.Load(genLoadExpression())
    }

    fun genStore(): BodyStatement.Store {
        return BodyStatement.Store(genStoreExpression(), genLoadExpression())
    }

    fun genCall(): BodyStatement.Call {
        return BodyStatement.Call(chooseFunction(), genArgs())
    }

    fun genSpawnThread(): BodyStatement.SpawnThread {
        return BodyStatement.SpawnThread(chooseFunction(), genArgs())
    }

    // TODO respect symbol's parameters size?
    fun genArgs(): List<LoadExpression> = List(distributions.numParameters.next(random)) { genLoadExpression() }

    fun genLoadExpression(): LoadExpression = when (distributions.access.next(random)) {
        AccessKind.LOCAL -> LoadExpression.Local(chooseLocal(), genPath())
        AccessKind.GLOBAL -> LoadExpression.Global(chooseGlobal(), genPath())
    }

    fun genStoreExpression(): StoreExpression = when (distributions.access.next(random)) {
        AccessKind.LOCAL -> StoreExpression.Local(chooseLocal(), genPath())
        AccessKind.GLOBAL -> StoreExpression.Global(chooseGlobal(), genPath())
    }

    fun genPath(): Path = Path(List(distributions.pathLength.next(random)) { random.nextInt(0, Int.MAX_VALUE) })

}