/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.inference.common.ClassReference
import org.jetbrains.kotlin.nj2k.inference.common.ContextCollector
import org.jetbrains.kotlin.nj2k.inference.common.State
import org.jetbrains.kotlin.nj2k.inference.common.hasUnknownLabel
import org.jetbrains.kotlin.psi.KtTypeElement

class MutabilityContextCollector(
    resolutionFacade: ResolutionFacade,
    private val converterContext: NewJ2kConverterContext
) : ContextCollector(resolutionFacade) {
    override fun ClassReference.getState(typeElement: KtTypeElement?): State = when {
        typeElement == null -> State.UNUSED
        typeElement.hasUnknownLabel(converterContext) { it.unknownMutability } -> State.UNKNOWN
        else -> State.UNUSED
    }
}