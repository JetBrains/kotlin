/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.dsl

class Program(
    val definitions: List<Definition>,
    val mainBody: Body,
)

typealias EntityId = Int

sealed interface Definition {
    val targetLanguage: TargetLanguage

    class Class(
        override val targetLanguage: TargetLanguage,
        val fields: List<Field>,
    ) : Definition

    class Function(
        override val targetLanguage: TargetLanguage,
        val parameters: List<Parameter>,
        val body: BodyWithReturn,
    ) : Definition

    class Global(
        override val targetLanguage: TargetLanguage,
        val field: Field,
    ) : Definition
}

sealed interface TargetLanguage {
    object Kotlin : TargetLanguage
    object ObjC : TargetLanguage
}

object Field

object Parameter

class Body(
    val statements: List<BodyStatement>,
)

sealed interface BodyStatement {
    class Alloc(val classId: EntityId, val args: List<LoadExpression>) : BodyStatement
    class Load(val from: LoadExpression) : BodyStatement
    class Store(val to: StoreExpression, val from: LoadExpression) : BodyStatement
    class Call(val functionId: EntityId, val args: List<LoadExpression>) : BodyStatement
    class SpawnThread(val functionId: EntityId, val args: List<LoadExpression>) : BodyStatement
}

sealed interface LoadExpression {
    object Default : LoadExpression
    class Local(val localId: EntityId, val path: Path) : LoadExpression
    class Global(val globalId: EntityId, val path: Path) : LoadExpression
}

class Path(val accessors: List<EntityId>)

sealed interface StoreExpression {
    class Local(val localId: EntityId, val path: Path) : StoreExpression
    class Global(val globalId: EntityId, val path: Path) : StoreExpression
}

class BodyWithReturn(
    val body: Body,
    val returnExpression: LoadExpression,
)