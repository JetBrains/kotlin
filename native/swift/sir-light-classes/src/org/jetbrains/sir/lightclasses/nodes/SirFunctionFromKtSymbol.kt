/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.components.containingSymbol
import org.jetbrains.kotlin.analysis.api.export.utilities.isSuspend
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer.KotlinNameType
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import kotlin.lazy

internal open class SirFunctionFromKtSymbol(
    override val ktSymbol: KaFunctionSymbol,
    override val sirSession: SirSession,
) : SirFunction(), SirFromKtSymbol<KaFunctionSymbol> {

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
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
        ktSymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirFunction>? by lazy { computeIsOverride() }

    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel && (ktSymbol as KaNamedFunctionSymbol).let { !it.isStatic }

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality

    override val fixity: SirFixity?
        get() = null

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null || isAsync) SirType.any else SirType.never

    override val isAsync: Boolean get() = ktSymbol.isSuspend

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val fqName = bridgeFqName ?: return@lazyWithSessions null
        val suffix = ""
        val baseName = fqName.baseBridgeName + suffix

        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter("", "receiver", it.type)
        }

        // For F-bounded methods, use the interface type as self type to generate direct cast
        val effectiveSelfType = computeFBoundedInterfaceSirType() ?: selfType

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = contextParameters + listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                SirParameter("", "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = contextParameters,
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "_out_error", it)
            },
            isAsync = isAsync,
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        bridgeProxy?.createSirBridges {
            val implicitArgs = contextParameters + listOfNotNull(extensionReceiverParameter)
            val actualArgs = argNames.drop(implicitArgs.size)
            val argumentsString = actualArgs.joinToString()
            val castSuffix = (ktSymbol.returnType is KaTypeParameterType && ktSymbol.isTopLevel)
                .takeIf { it }
                ?.let {
                    val fqName = typeNamer.kotlinFqName(this@SirFunctionFromKtSymbol.returnType, KotlinNameType.FQN)
                    " as? $fqName"
                } ?: ""

            buildCall("($argumentsString)$castSuffix")
        }.orEmpty()
    }

    /**
     * For methods on F-bounded classes that override interface methods, computes the interface SirType
     * to use as the self parameter type. This allows the bridge to cast self directly to the interface type
     * (e.g., "Comparable<Any?>") instead of first casting to the class type and then to the interface.
     * Returns null if not applicable (not an F-bounded method).
     */
    private fun computeFBoundedInterfaceSirType(): SirType? = withSessions {
        // Only for instance methods on F-bounded classes
        if (!isInstance) return@withSessions null
        val containingClass = (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>() ?: return@withSessions null
        if (!containingClass.hasFBoundedTypeParameters()) return@withSessions null

        // Find the interface that declares this method
        val overriddenInterfaceMethod = ktSymbol.allOverriddenSymbols
            .filterIsInstance<KaNamedFunctionSymbol>()
            .firstOrNull { overridden ->
                val containingSymbol = overridden.containingSymbol
                containingSymbol is KaClassSymbol && containingSymbol.classKind == KaClassKind.INTERFACE
            } ?: return@withSessions null

        val interfaceSymbol = overriddenInterfaceMethod.containingSymbol as? KaNamedClassSymbol
            ?: return@withSessions null

        // Get the SirProtocol for the interface and wrap it in SirExistentialType
        val sirProtocol = interfaceSymbol.toSir().allDeclarations.firstIsInstanceOrNull<SirProtocol>()
            ?: return@withSessions null

        SirExistentialType(sirProtocol)
    }

    override var body: SirFunctionBody?
        set(_) {}
        get() = bridgeProxy?.createSwiftInvocation { "return $it" }?.let(::SirFunctionBody)
}
