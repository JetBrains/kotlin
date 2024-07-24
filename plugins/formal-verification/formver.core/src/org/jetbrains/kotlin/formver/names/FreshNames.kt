/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : MangledName {
    override val mangledType: String
        get() = "anon"
    override val mangledBaseName: String
        get() = n.toString()
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : MangledName {
    override val mangledBaseName: String
        get() = "ret"
}

data class ReturnVariableName(val n: Int) : MangledName {
    override val mangledType: String
        get() = "ret"
    override val mangledBaseName: String
        get() = n.toString()
}

data object ThisReceiverName : MangledName {
    override val mangledBaseName: String
        get() = "this"
}

data object SetterValueName : MangledName {
    override val mangledBaseName: String
        get() = "value"
}

data class SpecialName(override val mangledBaseName: String) : MangledName {
    override val mangledType: String
        get() = "sp"
}

abstract class NumberedLabelName(override val mangledScope: String, n: Int) : MangledName {
    override val mangledType: String
        get() = "lbl"
    override val mangledBaseName: String = n.toString()
}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("continue", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("try_exit", n)

data class TypeName(override val mangledBaseName: String) : MangledName {
    override val mangledType: String
        get() = "T"
}
