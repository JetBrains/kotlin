/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.frontend.api.symbols.*

sealed class CallInfo {
    abstract val isSuspendCall: Boolean
    abstract val targetFunction: KtFunctionLikeSymbol?
}

data class VariableAsFunctionCallInfo(val target: KtVariableLikeSymbol, override val isSuspendCall: Boolean) : CallInfo() {
    override val targetFunction: KtFunctionLikeSymbol? = null
}

data class VariableAsFunctionLikeCallInfo(val target: KtVariableLikeSymbol, val invokeFunction: KtFunctionSymbol) : CallInfo() {
    override val isSuspendCall: Boolean get() = invokeFunction.isSuspend
    override val targetFunction get() = invokeFunction
}

data class FunctionCallInfo(override val targetFunction: KtFunctionLikeSymbol) : CallInfo() {
    override val isSuspendCall: Boolean
        get() = when (targetFunction) {
            is KtFunctionSymbol -> targetFunction.isSuspend
            else -> false
        }
}