/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

abstract class BeforeElementDiagnosticCollectionHandler: FirSessionComponent {
    abstract fun beforeCollectingForElement(element: FirElement)
}

val FirSession.beforeElementDiagnosticCollectionHandler: BeforeElementDiagnosticCollectionHandler? by FirSession.nullableSessionComponentAccessor()
