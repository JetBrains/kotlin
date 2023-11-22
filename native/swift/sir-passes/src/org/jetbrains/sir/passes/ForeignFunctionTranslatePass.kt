/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.visitors.SirTransformer

class ForeignFunctionTranslatePass : SirTransformer<Unit>() {
    override fun <E : SirElement> transformElement(element: E, data: Unit): E {
        return element
    }

    override fun transformModule(module: SirModule, data: Unit): SirModule {
        module.transformChildren(this, data)
        return module
    }

    override fun transformForeignFunction(foreignFunction: SirForeignFunction, data: Unit): SirDeclaration {
        val foreignOrigin = foreignFunction.origin
        if (foreignOrigin is KotlinFunctionSirOrigin) {
            return buildFunction {
                origin = foreignOrigin
                visibility = SirVisibility.PUBLIC
                parameters.addAll(foreignOrigin.parameters)
                returnType = foreignOrigin.returnType
                name = foreignOrigin.fqName.last()
            }
        } else {
            error("Unsupported origin: $foreignOrigin")
        }
    }
}