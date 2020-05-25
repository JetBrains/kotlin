/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

// TODO: inline?
data class CirFunctionModifiers(
    var isOperator: Boolean,
    var isInfix: Boolean,
    var isInline: Boolean,
    var isTailrec: Boolean,
    var isSuspend: Boolean,
    var isExternal: Boolean
)
