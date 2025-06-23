/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.fuzzer

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Body
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.BodyStatement
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.BodyWithReturn
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Definition
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Field
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.LoadExpression
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Parameter
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Path
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Program
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.StoreExpression
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.TargetLanguage

fun generate(seed: UInt): Program {
    return Generator(seed.toInt()).genProgram()
}

private class Generator(seed: Int = 0, val distributions: Distributions = Distributions()) {
    private val random = kotlin.random.Random(seed)

    class Distributions {
        val numDefinitions = Distribution.uniformalBetween(0, 100)
        val language = Distribution.uniform(
            TargetLanguage.Kotlin,
            TargetLanguage.ObjC
        )
        val definition = Distribution.uniform<DefinitionKind>()
        val numFields = CumulativeFun.uniform(
            0.0 to 0,
            0.1 to 0,
            0.5 to 2,
            0.9 to 10,
            0.999 to 1000,
            1.0 to Int.MAX_VALUE
        )
        val numParameters = CumulativeFun.uniform(
            0.0 to 0,
            0.999 to 10,
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
        val pathLength = CumulativeFun.uniform(
            0.0 to 0,
            0.1 to 0,
            0.999 to 4,
            1.0 to 100
        )
        val access = Distribution.uniform<AccessKind>() // FIXME maybe more locals then globals?
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

    private val numDefinitions: Map<DefinitionKind, Int> = DefinitionKind.entries.associateWith { 1 } +
            (1 until distributions.numDefinitions.next(random) + 1).map {
                distributions.definition.next(random)
            }.groupBy { it }.mapValues { it.value.size }

    private fun genDefinitions(): List<Definition> {
        val globals = List(numDefinitions[DefinitionKind.GLOBAL]!!) { genGlobal() }
        val classes = List(numDefinitions[DefinitionKind.CLASS]!!) { genClass() }
        val functions = List(numDefinitions[DefinitionKind.FUNCTION]!!) { genFunction() }
        return globals + classes + functions
    }

    fun genProgram(): Program = Program(
        definitions = genDefinitions(),
        mainBody = genBody(),
    )

    fun genClass(): Definition.Class {
        val lang = genLanguage()
        return Definition.Class(
            lang,
            genFields()
        )
    }

    fun genFunction(): Definition.Function {
        val lang = genLanguage()
        return Definition.Function(
            lang,
            genParameters(),
            genBodyWithReturn(),
        )
    }

    fun genGlobal(): Definition.Global {
        val lang = genLanguage()
        return Definition.Global(lang, Field)
    }

    fun genLanguage(): TargetLanguage = distributions.language.next(random)

    fun genFields(): List<Field> =
        List(distributions.numFields.next(random)) { Field }

    fun genParameters(): List<Parameter> =
        List(distributions.numParameters.next(random)) { Parameter }

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
    // FIXME can be empty
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

    // FIXME respect target's parameters size?
    fun genArgs(): List<LoadExpression> = List(distributions.numParameters.next(random)) { genLoadExpression() }

    fun genLoadExpression(): LoadExpression = when (distributions.access.next(random)) {
        AccessKind.LOCAL -> LoadExpression.Local(chooseLocal(), genPath())
        AccessKind.GLOBAL -> LoadExpression.Global(chooseGlobal(), genPath())
    }

    fun genStoreExpression(): StoreExpression = when (distributions.access.next(random)) {
        AccessKind.LOCAL -> StoreExpression.Local(chooseLocal(), genPath())
        AccessKind.GLOBAL -> StoreExpression.Global(chooseGlobal(), genPath())
    }

    fun genPath(): Path = Path(List(distributions.pathLength.next(random)) {
        random.nextInt(0, Int.MAX_VALUE)
    })

}