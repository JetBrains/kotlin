/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension.Factory

class PluginAdditionalCheckers(session: FirSession, logLevel: LogLevel) : FirAdditionalCheckersExtension(session) {
    companion object {
        fun getFactory(logLevel: LogLevel): Factory {
            return Factory { session -> PluginAdditionalCheckers(session, logLevel) }
        }
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = setOf(ViperPoweredDeclarationChecker(session, logLevel))
    }
}

