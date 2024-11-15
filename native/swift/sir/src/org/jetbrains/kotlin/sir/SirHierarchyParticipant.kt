/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirHierarchyParticipant : SirElement {
    fun onParentChange(from: SirDeclarationParent?, to: SirDeclarationParent) {
        (this as? SirDeclarationContainer)?.declarations?.forEach { it.onParentChange(from, to) }
    }
}
