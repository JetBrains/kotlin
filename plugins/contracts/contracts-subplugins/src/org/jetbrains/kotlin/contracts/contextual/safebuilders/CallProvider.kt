/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.model.ContextProvider
import org.jetbrains.kotlin.contracts.contextual.util.FunctionAndThisInstanceHolder
import org.jetbrains.kotlin.psi.KtElement

internal data class CallProvider(val instanceHolder: FunctionAndThisInstanceHolder, val sourceElement: KtElement) : ContextProvider {
    override val family = CallFamily
}