/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.ClassId

// TODO: find a better name for it
sealed class PathUnique
data class Path(val symbol: FirVariableSymbol<FirVariable>) : PathUnique()
data class Level(val level: Set<UniqueLevel>) : PathUnique()

interface UniqueCheckerContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector
    val session: FirSession
    val uniqueId: ClassId
    val uniqueStack: ArrayDeque<ArrayDeque<PathUnique>>

    fun resolveUniqueAnnotation(declaration: FirDeclaration): UniqueLevel
}
