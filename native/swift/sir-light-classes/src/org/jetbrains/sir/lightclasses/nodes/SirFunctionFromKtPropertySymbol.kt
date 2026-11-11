/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirFixity
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirGenericDeclaration
import org.jetbrains.kotlin.sir.SirGetterFunction
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirSetterFunction
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeConstraint
import org.jetbrains.kotlin.sir.SirTypeParameter
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildFunctionCopy
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionBuilder
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinPropertyAccessorOrigin
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.sirModality
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*
import kotlin.getValue
import kotlin.lazy

internal class SirGetterFunctionFromKtSymbol(
    override val ktPropertySymbol: KaPropertySymbol,
    override val ktSymbol: KaPropertyGetterSymbol,
    override val sirSession: SirSession,
) : SirFunctionFromKtPropertySymbol(), SirGetterFunction {
    override val getter: SirGetterFunctionFromKtSymbol get() = this
    override var setter: SirSetterFunctionFromKtSymbol? = null
}

internal class SirSetterFunctionFromKtSymbol(
    override val ktPropertySymbol: KaPropertySymbol,
    override val ktSymbol: KaPropertySetterSymbol,
    override val sirSession: SirSession,
    override val getter: SirGetterFunctionFromKtSymbol,
) : SirFunctionFromKtPropertySymbol(), SirSetterFunction {
    override val setter: SirSetterFunctionFromKtSymbol get() = this
}

/**
 * Kotlin extension property accessors in Swift
 */
internal sealed class SirFunctionFromKtPropertySymbol : SirFunction(), SirFromKtSymbol<KaPropertyAccessorSymbol> {
    abstract val ktPropertySymbol: KaPropertySymbol

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin by lazy {
        KotlinPropertyAccessorOrigin(ktSymbol, ktPropertySymbol)
    }
    val variableName: String by lazyWithSessions {
        ktPropertySymbol.sirDeclarationName()
    }
    override val name: String by lazy {
        val prefix = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "get"
            is KaPropertySetterSymbol -> "set"
        }
        prefix + variableName.replaceFirstChar { it.titlecase() }
    }

    private val translatedTypeParams: TranslatedTypeParameters by lazyWithSessions {
        translateTypeParametersAndConstraints(
            declarationSymbol = ktPropertySymbol,
            ownKtTypeParams = ktPropertySymbol.typeParameters,
            enclosingScope = enclosingTypeParameterScope(),
            callableSymbol = ktPropertySymbol,
        )
    }

    override val typeParameters: List<SirTypeParameter>
        get() = translatedTypeParams.typeParameters

    override val constraints: List<SirTypeConstraint>
        get() = translatedTypeParams.constraints

    internal val contextParameters: Pair<SirParameter, List<SirParameter>>? by lazy {
        translateContextParameters()
    }
    override val contextParameter: SirParameter? get() = contextParameters?.first
    override val extensionReceiverParameter: SirParameter? by lazy {
        translateExtensionParameter()
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters()
    }
    override val returnType: SirType by lazy {
        translateReturnType()
    }
    private val kdocElements: KDocElements? by lazyWithSessions {
        KDocElements(this)
    }
    override val documentation: String? by lazyWithSessions {
        translateDocumentation(kdocElements)
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

    private val baseAttributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirFunctionFromKtPropertySymbol.translatedAttributes)
            if (overrideStatus is OverrideStatus.Conflicts) {
                add(SirAttribute.NonOverride)
            }
            replaceOrAddPropagatedUnavailability {
                buildList {
                    contextParameter?.type?.let(::add)
                    extensionReceiverParameter?.type?.let(::add)
                    addAll(parameters.map { it.type })
                    add(returnType)
                }.flatMap { it.unavailableTypes }
            }
            addDocumentationVisibility(kdocElements)
        }
    }

    private val isSelfUnavailable: Boolean
        get() = baseAttributes.any { it is SirAttribute.Available && it.unavailable }

    private val canRequestInlinable: Boolean by lazyWithSessions {
        val parentDecl = parent as? SirDeclaration
        val isGenericContext = typeParameters.isNotEmpty() ||
            (parent as? SirGenericDeclaration)?.typeParameters?.isNotEmpty() == true
        isGenericContext &&
            parent !is SirProtocol &&
            visibility == SirVisibility.PUBLIC &&
            modality == SirModality.FINAL &&
            !isAsync &&
            errorType == SirType.never &&
            contextParameter == null &&
            !isSelfUnavailable &&
            (parentDecl?.isUnavailable != true) &&
            (parentDecl?.attributes.orEmpty().none { it is SirAttribute.SPI || it is SirAttribute.Available }) &&
            baseAttributes.none { it is SirAttribute.SPI || it is SirAttribute.Available }
    }

    override val attributes: List<SirAttribute> by lazy {
        if (canRequestInlinable && bridgeProxy?.isSwiftInlinable == true) {
            baseAttributes + SirAttribute.Inlinable
        } else {
            baseAttributes
        }
    }

    override val errorType: SirType get() = if (ktPropertySymbol.throwsAnnotation != null) SirType.any else SirType.never

    override val isAsync: Boolean get() = false

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (isSelfUnavailable) return@lazyWithSessions null
        val fqName = ktPropertySymbol
            .callableId?.asSingleFqName() ?: return@lazyWithSessions null

        val suffix = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "_get"
            is KaPropertySetterSymbol -> "_set"
        }

        val baseName = fqName.baseBridgeName + suffix

        val contextParameters = contextParameters?.second ?: emptyList()
        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter(null, "receiver", it.type)
        }

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            kotlinOptIns = ktSymbol.allRequiredOptIns,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                val effectiveSelfType = interfaceSelfType(ktPropertySymbol) ?: selfType
                SirParameter(null, "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = contextParameters,
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter(null, "_out_error", it)
            },
            isAsync = false,
            isInlinable = canRequestInlinable,
        )
    }

    context(_: KaSession, _: SirSession)
    private fun buildPropertyAccessorForwardCall(): BridgeFunctionBuilder.() -> String = {
        val args = argNames
        val enclosingClassSymbol = ktPropertySymbol.containingDeclaration as? KaClassSymbol
        val useLocalBridge = (sirSession.enableGenericsSupport && ktPropertySymbol.typeParameters.isNotEmpty()) ||
            hasNonTrivialKotlinBridgeBounds(sirSession, enclosingClassSymbol, ktPropertySymbol.typeParameters)
        when (ktSymbol) {
            is KaPropertyGetterSymbol -> {
                val expectedParameters = (if (extensionReceiverParameter != null) 1 else 0) + contextParameters.size
                require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
                if (useLocalBridge) {
                    buildLocalGenericBridgeCall(enclosingClassSymbol, ktPropertySymbol, this@SirFunctionFromKtPropertySymbol.returnType)
                } else {
                    buildCall("")
                }
            }
            is KaPropertySetterSymbol -> {
                val contextParameterCount = contextParameters.size
                val expectedParameters = (if (extensionReceiverParameter != null) 2 else 1) + contextParameterCount
                require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
                if (useLocalBridge) {
                    buildLocalGenericBridgeCall(
                        enclosingClassSymbol,
                        ktPropertySymbol,
                        SirType.void,
                        isSetter = true,
                        setterParamExpr = args.last(),
                    )
                } else {
                    buildCall(" = ${args.last()}")
                }
            }
        }
    }

    /** The name of the Kotlin accessor, e.g. `<get-foo>`. */
    private val kotlinAccessorName: String
        get() = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "<get-${ktPropertySymbol.name.asString()}>"
            is KaPropertySetterSymbol -> "<set-${ktPropertySymbol.name.asString()}>"
        }

    private val isAbstractKotlinProperty: Boolean
        get() = ktPropertySymbol.modality == KaSymbolModality.ABSTRACT

    override val bridges: List<SirBridge> by lazyWithSessions {
        val proxy = bridgeProxy ?: return@lazyWithSessions emptyList()
        val forwardCall = buildPropertyAccessorForwardCall()
        buildList {
            addAll(proxy.createSirBridges(nonVirtualTargetMethod = null, forwardCall))
            if (needsNonVirtualForwardBridge() && !isAbstractKotlinProperty) {
                add(proxy.createDirectDispatchForwardBridge(kotlinAccessorName, forwardCall))
            }
            if (needsReverseBridge()) {
                addAll(
                    proxy.createReverseSirBridges(
                        targetClassFqName = ktPropertySymbol.reverseBridgeTargetClassFqName,
                        targetMethodName = kotlinAccessorName,
                        swiftDynamicCall = this@SirFunctionFromKtPropertySymbol::reverseBridgeSwiftCall,
                        swiftDeprecation = effectiveReverseBridgeDeprecation(),
                        openExistentialProtocol = openExistentialProtocolForReverseBridge(),
                    )
                )
            }
        }
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = withSessions { bridgeProxy?.let { forwardBridgedBody(it, isAbstractKotlinProperty, memberKind = "member", memberName = name) } }

    internal fun directDispatchProtocolWitnessOrNull(): SirFunction? = withSessions {
        if (parent !is SirProtocol) return@withSessions null
        if (!isInstance || isUnavailable || isAbstractKotlinProperty) return@withSessions null
        if (!needsReverseBridge()) return@withSessions null
        val proxy = bridgeProxy ?: return@withSessions null
        val forwardCall = buildPropertyAccessorForwardCall()

        buildFunctionCopy(this@SirFunctionFromKtPropertySymbol) {
            origin = SirOrigin.Trampoline(this@SirFunctionFromKtPropertySymbol)
            isOverride = false
            modality = SirModality.UNSPECIFIED
            bridges.clear()
            bridges.add(proxy.createDirectDispatchForwardBridge(kotlinAccessorName, forwardCall))
            body = SirFunctionBody(proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" })
        }
    }
}
