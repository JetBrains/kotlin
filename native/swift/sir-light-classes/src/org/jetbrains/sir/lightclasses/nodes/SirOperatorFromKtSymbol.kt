/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirFixity
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirGetter
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirScopeDefiningDeclaration
import org.jetbrains.kotlin.sir.SirSetter
import org.jetbrains.kotlin.sir.SirSubscript
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.allParameters
import org.jetbrains.kotlin.sir.util.name
import kotlin.getValue

internal interface SirOperatorAuxiliaryDeclaration

internal open class SirRenamedFunction(
    override val ktSymbol: KaNamedFunctionSymbol,
    override val sirSession: SirSession,
    val nameTransform: (String) -> String = { "_$it" }
) : SirFunctionFromKtSymbol(ktSymbol, sirSession) {
    override val name: String get() = nameTransform(super.name)
}

internal abstract class SirClassOperatorTrampolineFunction(
    val source: SirFunction,
) : SirFunction(), SirOperatorAuxiliaryDeclaration {
    override var parent: SirDeclarationParent
        get() = source.parent
        set(newValue) {}

    override val origin: SirOrigin get() = SirOrigin.Trampoline(source)
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val returnType: SirType get() = source.returnType
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = false
    override val modality: SirModality get() = SirModality.FINAL
    override val attributes: List<SirAttribute> get() = source.attributes
    override val contextParameters: List<SirParameter> get() = source.contextParameters
    override val extensionReceiverParameter: SirParameter? get() = source.extensionReceiverParameter
    override val errorType: SirType get() = source.errorType
    override val isAsync: Boolean get() = source.isAsync
    override val parameters: List<SirParameter>
        get() = listOf(
            SirParameter(argumentName = "this", type = selfType)
        ) + source.parameters

    override val fixity: SirFixity?
        get() = SirFixity.INFIX

    override val bridges: List<SirBridge> get() = emptyList()

    override var body: SirFunctionBody?
        get() = SirFunctionBody(
            listOf(
                "this.${source.name}(${this.allParameters.drop(1).joinToString { it.forward ?: error("unreachable") }})"
            )
        )
        set(_) = Unit
}

internal class SirBinaryMathOperatorTrampolineFunction(
    source: SirFunction,
    override val name: String,
) : SirClassOperatorTrampolineFunction(source) {
    override val fixity: SirFixity?
        get() = SirFixity.INFIX
}

internal class SirUnaryMathOperatorTrampolineFunction(
    source: SirFunction,
    override val name: String,
) : SirClassOperatorTrampolineFunction(source) {
    override val fixity: SirFixity?
        get() = SirFixity.PREFIX
}

internal class SirComparisonOperatorTrampolineFunction(
    source: SirFunction,
    override val name: String,
) : SirClassOperatorTrampolineFunction(source) {
    override val returnType: SirType get() = SirNominalType(SirSwiftModule.bool)

    override val fixity: SirFixity?
        get() = SirFixity.INFIX

    override var body: SirFunctionBody?
        get() = SirFunctionBody(
            listOf(
                super.body!!.statements.single() + " $name 0"
            )
        )
        set(_) = Unit
}

internal class SirSubscriptTrampoline(
    val getterFunction: SirFunction,
    val setterFunction: SirFunction?,
) : SirSubscript(), SirOperatorAuxiliaryDeclaration {
    override var parent: SirDeclarationParent
        get() = getterFunction.parent
        set(newValue) {}
    override val origin: SirOrigin get() = SirOrigin.Trampoline(getterFunction)

    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? get() = getterFunction.documentation
    override val attributes: List<SirAttribute> get() = getterFunction.attributes
    override val isOverride: Boolean = false
    override val isInstance: Boolean = true
    override val modality: SirModality = SirModality.FINAL
    override val parameters: List<SirParameter> get() = getterFunction.parameters
    override val returnType: SirType get() = getterFunction.returnType

    override val getter: SirGetter by lazy {
        buildGetter {
            parent = this@SirSubscriptTrampoline
            origin = SirOrigin.Trampoline(getterFunction)
            body = SirFunctionBody(
                listOf(
                    "_get(${parameters.joinToString { it.forward ?: error("unreachable") }})"
                )
            )
        }
    }
    override val setter: SirSetter? by lazy {
        setterFunction?.let { setterFunction ->
            buildSetter {
                parent = this@SirSubscriptTrampoline
                origin = SirOrigin.Trampoline(setterFunction)
                parameterName = setterFunction.parameters.last().name ?: "newValue"
                body = SirFunctionBody(
                    listOf(
                        "_set(${parameters.joinToString { it.forward ?: error("unreachable") }}, ${setterFunction.parameters.last().forward})"
                    )
                )
            }
        }
    }
}

private val SirParameter.forward: String? get() = this.name?.let { name -> this.argumentName?.let { "$it: $name" } ?: name }

private val SirFunction.selfType: SirType get() {
        return this.extensionReceiverParameter?.type
            ?: (this.parent as? SirScopeDefiningDeclaration)?.let { SirNominalType(it) }
            ?: error("No receiver type available for ${this.name}")
}
