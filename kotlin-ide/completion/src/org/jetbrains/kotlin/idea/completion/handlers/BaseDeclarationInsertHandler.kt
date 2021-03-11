/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.renderer.render

open class BaseDeclarationInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val name = (item.`object` as? DeclarationLookupObject)?.name
        if (name != null && !name.isSpecial) {
            val startOffset = context.startOffset
            if (startOffset > 0 && context.document.isTextAt(startOffset - 1, "`")) {
                context.document.deleteString(startOffset - 1, startOffset)
            }
            context.document.replaceString(context.startOffset, context.tailOffset, name.render())
        }
    }
}
