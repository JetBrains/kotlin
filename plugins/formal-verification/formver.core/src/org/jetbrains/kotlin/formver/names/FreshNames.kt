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
    override val mangled: String
        get() = "anonymous\$$n"
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : MangledName {
    override val mangled: String
        get() = "ret"
}

data class ReturnVariableName(val n: Int) : MangledName {
    override val mangled: String
        get() = "ret\$$n"
}

data object ThisReceiverName : MangledName {
    override val mangled: String
        get() = "this"
}

abstract class SpecialNameBase(name: String) : MangledName {
    override val mangled: String = "special\$$name"
}

data class SpecialName(val name: String) : SpecialNameBase(name)
data object GetterFunctionSubjectName : SpecialNameBase("get\$function\$subject")
data object ClassPredicateSubjectName : SpecialNameBase("class\$predicate\$subject")

data class GetterFunctionName(val className: MangledName, val fieldName: MangledName) : MangledName {
    override val mangled: String
        get() = "${className.mangled}\$get\$${fieldName.mangled}"
}

abstract class NumberedLabelName(kind: String, n: Int) : MangledName {
    override val mangled = "label\$$kind\$$n"
}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("continue", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("try_exit", n)
