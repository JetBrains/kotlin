/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.Key

class MoveDeclarationsEditorCookie(
    val data: MoveDeclarationsTransferableData,
    val bounds: RangeMarker,
    val modificationCount: Long
) {
    companion object {
        val KEY = Key<MoveDeclarationsEditorCookie>("MoveDeclarationsEditorCookie")
    }
}