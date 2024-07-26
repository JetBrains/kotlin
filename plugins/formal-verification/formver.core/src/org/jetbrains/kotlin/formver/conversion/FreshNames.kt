/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.viper.MangledName

/**
 * This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : MangledName {
    override val mangled: String
        get() = "anonymous\$$n"
}

data object ReturnVariableName : MangledName {
    override val mangled: String
        get() = "ret"
}

data object ThisReceiverName : MangledName {
    override val mangled: String
        get() = "this"
}

data class SpecialName(val name: String) : MangledName {
    override val mangled: String
        get() = "special\$$name"
}

data class InlineName(val inlineFunctionName: MangledName, val name: MangledName) : MangledName {
    override val mangled: String
        get() = "inline\$${inlineFunctionName.mangled}\$${name.mangled}"
}

data object ReturnLabelName : MangledName {
    override val mangled: String
        get() = "label\$ret"
}

data class InlineReturnLabelName(val inlineFunctionName: MangledName) : MangledName {
    override val mangled: String
        get() = "inline_label\$${inlineFunctionName.mangled}\$ret"
}

data class BreakLabelName(val n: Int) : MangledName {
    override val mangled: String
        get() = "label\$break\$$n"
}

data class ContinueLabelName(val n: Int) : MangledName {
    override val mangled: String
        get() = "label\$continue\$$n"
}