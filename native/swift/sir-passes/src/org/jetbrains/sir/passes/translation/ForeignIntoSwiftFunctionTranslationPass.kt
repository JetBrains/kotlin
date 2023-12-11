/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes.translation

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.KotlinFunction
import org.jetbrains.kotlin.sir.constants.*
import org.jetbrains.kotlin.sir.visitors.SirTransformerVoid
import org.jetbrains.sir.passes.SirPass
import java.lang.IllegalStateException


/**
 * Translates `SirForeignFunction` into `SirFunction`.
 * Works only with Nominal Types and top-level functions, currently.
 * If received `element` of different type than `SirForeignFunction`,
 * or `element` does not contain origin of type `SirOrigin.KotlinEntity.Function`,
 * returns original element.
 */
public class ForeignIntoSwiftFunctionTranslationPass : SirPass<SirElement, Nothing?> {

    private class Transformer : SirTransformerVoid() {
        override fun <E : SirElement> transformElement(element: E): E {
            element.transformChildren(this)
            return element
        }

        override fun transformForeignFunction(function: SirForeignFunction): SirDeclaration {
            val kotlinOrigin = (function.origin as? SirOrigin.ForeignEntity)?.entity as? KotlinFunction
                ?: return function
            return buildFunction {
                origin = function.origin
                visibility = function.visibility
                name = kotlinOrigin.fqName.last()
                kotlinOrigin.parameters.mapTo(parameters) { it.toSir() }

                returnType = kotlinOrigin.returnType.toSir()
            }.apply {
                parent = function.parent
            }
        }
    }

    override fun run(element: SirElement, data: Nothing?): SirElement = element.transform(Transformer())
}

private fun KotlinParameter.toSir(): SirParameter = SirParameter(
    argumentName = name,
    type = type.toSir(),
)

private fun KotlinType.toSir(): SirType = SirNominalType(
    type = when (this.name) {
        BYTE -> SirSwiftModule.int8
        SHORT -> SirSwiftModule.int16
        INT -> SirSwiftModule.int32
        LONG -> SirSwiftModule.int64
        BOOLEAN -> SirSwiftModule.bool
        DOUBLE -> SirSwiftModule.double
        FLOAT -> SirSwiftModule.float
        else -> throw IllegalStateException("unknown externally defined type")
    }
)
