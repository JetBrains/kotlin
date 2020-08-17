/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline.Companion.PARAMETER_USAGE_KEY
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline.Companion.TYPE_PARAMETER_USAGE_KEY
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents code to be inlined to replace usages of particular callable.
 * The expression should be preprocessed in the following way:
 * * Type arguments for all calls should be made explicit
 * * All external symbols to be imported should be either referenced via fully-qualified form or included into [fqNamesToImport]
 * * All usages of value parameters (of our callable) should be marked with [PARAMETER_USAGE_KEY] copyable user data (holds the name of the corresponding parameter)
 * * All usages of type parameters (of our callable) should be marked with [TYPE_PARAMETER_USAGE_KEY] copyable user data (holds the name of the corresponding type parameter)
 * Use [CodeToInlineBuilder.prepareCodeToInlineWithAdvancedResolution] or [CodeToInlineBuilder.prepareCodeToInline].
 */
class CodeToInline(
    val mainExpression: KtExpression?,
    val statementsBefore: List<KtExpression>,
    val fqNamesToImport: Collection<FqName>,
    val alwaysKeepMainExpression: Boolean,
    val extraComments: CommentHolder?,
) {
    companion object {
        val PARAMETER_USAGE_KEY: Key<Name> = Key("PARAMETER_USAGE")
        val TYPE_PARAMETER_USAGE_KEY: Key<Name> = Key("TYPE_PARAMETER_USAGE")
        val SIDE_RECEIVER_USAGE_KEY: Key<Unit> = Key("SIDE_RECEIVER_USAGE")
    }
}
