/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirFixity
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinPropertyAccessorOrigin
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.sirModality
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.OverrideStatus
import org.jetbrains.sir.lightclasses.utils.baseBridgeName
import org.jetbrains.sir.lightclasses.utils.computeIsOverride
import org.jetbrains.sir.lightclasses.utils.selfType
import org.jetbrains.sir.lightclasses.utils.translateContextParameters
import org.jetbrains.sir.lightclasses.utils.translateExtensionParameter
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import kotlin.getValue
import kotlin.lazy

/**
 * Kotlin extension property accessors in Swift
 */
internal class SirFunctionFromKtPropertySymbol(
    val ktPropertySymbol: KaPropertySymbol,
    override val ktSymbol: KaPropertyAccessorSymbol,
    override val sirSession: SirSession,
) : SirFunction(), SirFromKtSymbol<KaPropertyAccessorSymbol> {

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin by lazy {
        KotlinPropertyAccessorOrigin(ktSymbol, ktPropertySymbol)
    }
    override val name: String by lazyWithSessions {
        val prefix = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "get"
            is KaPropertySetterSymbol -> "set"
        }
        prefix + ktPropertySymbol.sirDeclarationName().replaceFirstChar { it.titlecase() }
    }
    override val contextParameters: List<SirParameter> by lazy {
        translateContextParameters()
    }
    override val extensionReceiverParameter: SirParameter? by lazy {
        translateExtensionParameter()
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters()
    }
    override val returnType: SirType by lazy {
        translateReturnType()
    }
    override val documentation: String? by lazyWithSessions {
        ktPropertySymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktPropertySymbol.getSirParent()
        }
        set(_) = Unit

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirFunction>? by lazy { computeIsOverride() }

    override val isInstance: Boolean
        get() = !ktPropertySymbol.isTopLevel && !ktPropertySymbol.isStatic

    override val modality: SirModality
        get() = ktPropertySymbol.modality.sirModality

    override val fixity: SirFixity?
        get() = null

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val errorType: SirType get() = if (ktPropertySymbol.throwsAnnotation != null) SirType.any else SirType.never

    override val isAsync: Boolean get() = false

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val fqName = ktPropertySymbol
            .callableId?.asSingleFqName() ?: return@lazyWithSessions null

        val suffix = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "_get"
            is KaPropertySetterSymbol -> "_set"
        }

        val baseName = fqName.baseBridgeName + suffix

        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter("", "receiver", it.type)
        }

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = contextParameters + listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                SirParameter("", "self", selfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = contextParameters,
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "_out_error", it)
            },
            isAsync = false,
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        bridgeProxy?.createSirBridges {
            val args = argNames
            when(ktSymbol) {
                is KaPropertyGetterSymbol -> {
                    val expectedParameters = contextParameters.size + (if (extensionReceiverParameter != null) 1 else 0)
                    require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
                    buildCall("")
                }
                is KaPropertySetterSymbol -> {
                    val expectedParameters = contextParameters.size + (if (extensionReceiverParameter != null) 2 else 1)
                    require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
                    buildCall(" = ${args.last()}")
                }
            }
        }.orEmpty()
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = bridgeProxy?.createSwiftInvocation { "return $it" }?.let(::SirFunctionBody)
}
