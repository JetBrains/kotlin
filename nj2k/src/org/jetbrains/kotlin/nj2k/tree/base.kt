/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.j2k.ast.Nullability

interface JKOperator {
    val token: JKOperatorToken
    val precedence: Int
}

interface JKOperatorToken {
    val text: String
}

interface JKKtOperatorToken : JKOperatorToken {
    val operatorName: String
}

interface JKQualifier

interface JKElement {
    val parent: JKElement?

    fun detach(from: JKElement)

    fun attach(to: JKElement)
}


interface JKBranchElement : JKElement {
    val children: List<Any>

    val valid: Boolean
    fun invalidate()
}

interface JKType {
    val nullability: Nullability
}