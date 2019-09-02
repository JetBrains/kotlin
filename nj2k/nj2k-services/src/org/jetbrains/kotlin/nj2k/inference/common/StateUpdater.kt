/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.common

import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

abstract class StateUpdater {
    fun updateStates(inferenceContext: InferenceContext) {
        if (inferenceContext.typeVariables.isEmpty()) return

        val deepComparator = Comparator<TypeElementBasedTypeVariable> { o1, o2 ->
            if (o1.typeElement.typeElement.isAncestor(o2.typeElement.typeElement)) 1 else -1
        }
        val typeVariablesSortedByDeep = inferenceContext.typeVariables
            .filterIsInstance<TypeElementBasedTypeVariable>()
            .sortedWith(deepComparator)

        runWriteAction {
            for (typeVariable in typeVariablesSortedByDeep) {
                typeVariable.updateState()
            }
        }
    }

    abstract fun TypeElementBasedTypeVariable.updateState()
}