/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeConstraint
import org.jetbrains.kotlin.sir.SirTypeParameter
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.substituteTypeParameters
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.swiftName

public class SirTrampolineExtensionFunction internal constructor(
    source: SirFunction,
    private val mapping: ExtensionMapping? = null,
) : SirTrampolineFunction(source) {
    public constructor(source: SirFunction) : this(source, null)

    override val isInstance: Boolean get() = true
    override val extensionReceiverParameter: SirParameter? get() = null

    override val typeParameters: List<SirTypeParameter>
        get() = mapping?.remainingFunctionTypeParameters ?: source.typeParameters

    override val constraints: List<SirTypeConstraint>
        get() = mapping?.remainingFunctionConstraints ?: source.constraints

    override val returnType: SirType
        get() = mapping?.let { source.returnType.substituteTypeParameters(it.typeSubstitution) } ?: source.returnType

    override val contextParameter: SirParameter?
        get() = source.contextParameter?.let { ctxParam ->
            val sub = mapping?.typeSubstitution.orEmpty()
            if (sub.isEmpty()) ctxParam else SirParameter(
                argumentName = ctxParam.argumentName,
                parameterName = ctxParam.parameterName,
                type = ctxParam.type.substituteTypeParameters(sub),
                origin = ctxParam.origin,
                isVariadic = ctxParam.isVariadic,
            )
        }

    override val parameters: List<SirParameter> by lazy {
        val sub = mapping?.typeSubstitution.orEmpty()
        source.parameters.mapIndexed { index, element ->
            val substType = element.type.substituteTypeParameters(sub)
            if (element.argumentName == null && element.parameterName == null) {
                SirParameter(parameterName = "_$index", type = substType, isVariadic = element.isVariadic)
            } else if (substType != element.type) {
                SirParameter(
                    argumentName = element.argumentName,
                    parameterName = element.parameterName,
                    type = substType,
                    origin = element.origin,
                    isVariadic = element.isVariadic,
                )
            } else {
                element
            }
        }
    }

    override var body: SirFunctionBody?
        get() = buildTrampolineToSource(source) {
            val recvName = source.extensionReceiverParameter!!.name!!.swiftIdentifier
            val recvAnnot = mapping?.receiverTypeAnnotation
            if (recvAnnot != null) {
                add("let $recvName: ${recvAnnot.swiftName} = self")
            } else {
                add("let $recvName = self")
            }
        }
        set(_) = Unit
}
