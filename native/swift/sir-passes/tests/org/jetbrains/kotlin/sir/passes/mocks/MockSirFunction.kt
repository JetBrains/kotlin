/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes.mocks

import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

class MockSirFunction(
    override val origin: SirOrigin = SirOrigin.Unknown,
    override val visibility: SirVisibility = SirVisibility.PUBLIC,
    override var parent: SirDeclarationParent,
    override val name: String,
    override val parameters: List<SirParameter>,
    override val returnType: SirType,
) : SirFunction() {
    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) = Unit
    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) = Unit

    override var body: SirFunctionBody? = null
}
