/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.nullability

import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.UnknownNullability
import org.jetbrains.kotlin.nj2k.inference.common.ClassReference
import org.jetbrains.kotlin.nj2k.inference.common.ContextCollector
import org.jetbrains.kotlin.nj2k.inference.common.State
import org.jetbrains.kotlin.nj2k.inference.common.getLabel
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class NullabilityContextCollector(
    resolutionFacade: ResolutionFacade,
    private val converterContext: NewJ2kConverterContext
) : ContextCollector(resolutionFacade) {
    override fun ClassReference.getState(typeElement: KtTypeElement?): State? {
        val hasUndefinedNullabilityLabel = typeElement
            ?.parent
            ?.safeAs<KtTypeReference>()
            ?.hasUndefinedNullabilityLabel(converterContext)
            ?: false
        return when {
            typeElement == null -> State.UNKNOWN
            hasUndefinedNullabilityLabel -> State.UNKNOWN
            typeElement is KtNullableType -> State.UPPER
            else -> State.LOWER
        }
    }


    private fun KtTypeReference.hasUndefinedNullabilityLabel(context: NewJ2kConverterContext) =
        getLabel()?.let { label ->
            context.elementsInfoStorage.getInfoForLabel(label).orEmpty().contains(UnknownNullability)
        } ?: false
}