/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.psi.KtCallableDeclaration

abstract class AbstractImplicitTypeInspection(
    additionalChecker: (KtCallableDeclaration, AbstractImplicitTypeInspection) -> Boolean
) : IntentionBasedInspection<KtCallableDeclaration>(
    SpecifyTypeExplicitlyIntention::class,
    { element, inspection ->
        with(inspection as AbstractImplicitTypeInspection) {
            element.typeReference == null && additionalChecker(element, inspection)
        }
    }
) {
    override fun inspectionTarget(element: KtCallableDeclaration) = element.nameIdentifier
}