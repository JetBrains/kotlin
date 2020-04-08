/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.nullability

import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.inference.common.*
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.types.typeUtil.isUnit

class NullabilityContextCollector(
    resolutionFacade: ResolutionFacade,
    private val converterContext: NewJ2kConverterContext
) : ContextCollector(resolutionFacade) {
    override fun ClassReference.getState(typeElement: KtTypeElement?): State = when {
        descriptor?.defaultType?.isUnit() == true -> State.LOWER
        typeElement == null -> State.UNKNOWN
        typeElement.hasUnknownLabel(converterContext) { it.unknownNullability } -> State.UNKNOWN
        typeElement is KtNullableType -> State.UPPER
        else -> State.LOWER
    }
}