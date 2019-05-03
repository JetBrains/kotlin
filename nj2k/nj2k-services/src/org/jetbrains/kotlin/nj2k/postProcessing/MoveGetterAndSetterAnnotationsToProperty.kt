/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.intentions.addUseSiteTarget
import org.jetbrains.kotlin.nj2k.NewJ2kPostProcessing
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

class MoveGetterAndSetterAnnotationsToProperty : NewJ2kPostProcessing {
    override val writeActionNeeded: Boolean = true

    override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
        if (element !is KtProperty) return null
        if (element.accessors.isEmpty()) return null
        return {
            for (accessor in element.accessors.sortedBy { it.isGetter }) {
                for (entry in accessor.annotationEntries) {
                    element.addAnnotationEntry(entry).also {
                        it.addUseSiteTarget(
                            if (accessor.isGetter) AnnotationUseSiteTarget.PROPERTY_GETTER
                            else AnnotationUseSiteTarget.PROPERTY_SETTER,
                            element.project
                        )
                    }
                }
                accessor.annotationEntries.forEach { it.delete() }
            }
        }
    }
}