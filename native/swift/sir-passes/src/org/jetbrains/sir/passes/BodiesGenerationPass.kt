/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes

import org.jetbrains.kotlin.sir.KotlinFunctionSirOrigin
import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.visitors.SirVisitor

class BodiesGenerationPass : SirVisitor<Unit, Unit>() {
    val nameCounter = mutableMapOf<String, Int>()

    override fun visitElement(element: SirElement, data: Unit) {
        element.acceptChildren(this, data)
    }

    override fun visitFunction(function: SirFunction, data: Unit) {
        val origin = function.origin
        if (origin !is KotlinFunctionSirOrigin) return
        val bridgeName = origin.fqName.joinToString(separator = "_")
        val bridgeCounter = nameCounter.getOrPut(bridgeName) { 0 }
        nameCounter[bridgeName] = bridgeCounter + 1
        function.body = SirFunctionBody("${bridgeName}_$bridgeCounter", function.parameters)
    }
}