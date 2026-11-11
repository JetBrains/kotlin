/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.analysis.api.types.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.source.InnerInitSource
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.*
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*

private val obj = SirParameter(
    null, "__kt", SirNominalType(SirSwiftModule.unsafeMutableRawPointer)
)

internal sealed class SirInitFromKtSymbol(
    override val ktSymbol: KaFunctionSymbol,
    override val sirSession: SirSession,
) : SirInit(), SirFromKtSymbol<KaFunctionSymbol> {

    override val visibility: SirVisibility by lazyWithSessions {
        ktSymbol.sirAvailability().visibility ?: error("$ktSymbol shouldn't be exposed to SIR")
    }

    override val origin: SirOrigin by lazyWithSessions {
        if (isInner(ktSymbol)) InnerInitSource(ktSymbol) else KotlinSource(ktSymbol)
    }
    override val parameters: List<SirParameter> by lazy {
        calculateParameters()
    }

    protected fun calculateParameters(): List<SirParameter> {
        return translateParameters() + listOfNotNull(getOuterParameterOfInnerClass())
    }

    private val kdocElements: KDocElements? by lazyWithSessions {
        KDocElements(this)
    }
    override val documentation: String? by lazyWithSessions {
        translateDocumentation(kdocElements)
    }

    override val isRequired: Boolean = false

    override val isConvenience: Boolean = false

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirInit>? by lazy { computeIsOverride() }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    protected val baseAttributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirInitFromKtSymbol.translatedAttributes)
            if (overrideStatus is OverrideStatus.Conflicts) {
                add(SirAttribute.NonOverride)
            }
            replaceOrAddPropagatedUnavailability { parameters.flatMap { it.type.unavailableTypes } }
            addDocumentationVisibility(kdocElements)
        }
    }

    protected val isSelfUnavailable: Boolean
        get() = baseAttributes.any { it is SirAttribute.Available && it.unavailable }

    override val attributes: List<SirAttribute> by lazy {
        if (isSwiftInlinableInit) {
            baseAttributes + SirAttribute.Inlinable
        } else {
            baseAttributes
        }
    }

    protected open val isSwiftInlinableInit: Boolean
        get() = false

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never

    override val isAsync: Boolean get() = false

    override val typeParameters: List<SirTypeParameter> get() = emptyList()

    override val constraints: List<SirTypeConstraint> get() = emptyList()

    protected val isBridged: Boolean
        get() = withSessions {
            (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>()?.let {
                !it.defaultType.isArrayOrPrimitiveArray
            } ?: false
        }

    protected val isAbstractClass: Boolean
        get() = withSessions {
            (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>()?.modality?.isAbstract() ?: false
        }

    protected val isInheritableClass: Boolean
        get() = withSessions {
            when ((parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>()?.modality) {
                KaSymbolModality.OPEN, KaSymbolModality.ABSTRACT -> true
                else -> false
            }
        }

}

private inline fun <reified T : KaFunctionSymbol> SirFromKtSymbol<T>.getOuterParameterOfInnerClass(): SirParameter? {
    val parameterName = "outer__" //Temporary solution until there is no generic parameter mangling
    return withSessions {
        val sirFromKtSymbol = this@getOuterParameterOfInnerClass
        if (sirFromKtSymbol is SirInitFromKtSymbol && isInner(sirFromKtSymbol)) {
            val outSymbol = (ktSymbol.containingSymbol?.containingSymbol as? KaNamedClassSymbol)
            val outType = outSymbol?.defaultType?.translateType(
                SirTypeVariance.INVARIANT,
                { error("Error translating type") },
                { error("Unsupported type") },
                {})
            outType?.run {
                SirParameter(argumentName = parameterName, type = this)
            }
        } else null
    }
}

internal class SirRegularInitFromKtSymbol(
    ktSymbol: KaConstructorSymbol,
    sirSession: SirSession,
) : SirInitFromKtSymbol(ktSymbol, sirSession) {
    override val isFailable: Boolean
        get() = false

    private val canRequestInlinable: Boolean by lazyWithSessions {
        val enclosingClass = parent as? SirAbstractClassFromKtSymbol ?: return@lazyWithSessions false
        visibility == SirVisibility.PUBLIC &&
            enclosingClass.typeParameters.isNotEmpty() &&
            !isInheritableClass &&
            !isAbstractClass &&
            enclosingClass.rawSuperClass.typeDeclaration == KotlinRuntimeModule.kotlinBase &&
            errorType == SirType.never &&
            origin !is InnerInitSource &&
            !enclosingClass.isUnavailable &&
            enclosingClass.attributes.none { it is SirAttribute.SPI || it is SirAttribute.Available } &&
            !isSelfUnavailable &&
            baseAttributes.none { it is SirAttribute.SPI || it is SirAttribute.Available }
    }

    override val isSwiftInlinableInit: Boolean by lazyWithSessions {
        canRequestInlinable &&
            bridgeAllocProxy?.isSwiftInlinable == true &&
            bridgeInitProxy?.isSwiftInlinable == true
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        val scopeDecl = parent as? SirScopeDefiningDeclaration
            ?: error("Encountered an Init that produces non-named type: $parent")
        val producingType: SirType = scopeDecl.parameterizedSelfNominalType()
        val enclosingClassSymbol = ktSymbol.containingDeclaration as? KaNamedClassSymbol
        val useLocalGenericHelper = enclosingClassSymbol != null &&
            hasNonTrivialKotlinBridgeBounds(sirSession, enclosingClassSymbol, emptyList())

        val abstractConstructorTarget: String? = "<init>".takeIf { isAbstractClass }

        buildList {
            addAll(
                bridgeAllocProxy?.createSirBridges {
                    if (useLocalGenericHelper) {
                        buildLocalGenericAllocBridgeCall(enclosingClassSymbol)
                    } else {
                        val args = argNames
                        "kotlin.native.internal.createUninitializedInstance<${
                            typeNamer.kotlinFqName(
                                producingType,
                                SirTypeNamer.KotlinNameType.PARAMETRIZED
                            )
                        }>(${args.joinToString()})${
                            if ((ktSymbol.containingDeclaration as KaNamedClassSymbol).isInline) " as Any?" else ""
                        }"
                    }
                }.orEmpty()
            )
            if (origin is InnerInitSource) {
                addAll(
                    bridgeInitProxy?.createSirBridges(nonVirtualTargetMethod = abstractConstructorTarget) {
                        val args = this.argNames

                        require(!kotlinFqName.parent().isRoot) {
                            "Expected qualified name with a dot, but were ${kotlinFqName.asString()} instead"
                        }
                        require(args.size >= 2) {
                            "Expected >=2 inner constructor arguments, but were ${args.size}: ${args.joinToString(",")}"
                        }
                        val outerClassName = kotlinFqName.parent()
                        val innerClassName = kotlinFqName.shortName()
                        val innerConstructorArgs = args.drop(1).dropLast(1).joinToString(", ")
                        val innerConstructorCall = "(${args.last()} as $outerClassName).$innerClassName($innerConstructorArgs)"

                        "kotlin.native.internal.initInstance(${args.first()}, $innerConstructorCall)"
                    }.orEmpty()
                )
            } else {
                addAll(
                    bridgeInitProxy?.createSirBridges(nonVirtualTargetMethod = abstractConstructorTarget) {
                        if (useLocalGenericHelper) {
                            buildLocalGenericInitBridgeCall(enclosingClassSymbol, ktSymbol)
                        } else {
                            val args = argNames
                            "kotlin.native.internal.initInstance(${args.first()}, ${
                                typeNamer.kotlinFqName(
                                    producingType,
                                    SirTypeNamer.KotlinNameType.PARAMETRIZED
                                )
                            }(${args.drop(1).joinToString()}))"
                        }
                    }.orEmpty()
                )
            }
        }
    }

    override var body: SirFunctionBody?
        set(_) {}
        get() = withSessions {
            val initDescriptor = bridgeInitProxy ?: return@withSessions null
            val scope = parent as? SirScopeDefiningDeclaration
            val scopeTypeName = scope?.parameterizedSelfNominalType()?.swiftName
            val isGenericClass = (scope as? SirGenericDeclaration)?.typeParameters?.isNotEmpty() == true

            return@withSessions SirFunctionBody(buildList {
                if (isAbstractClass) {
                    scope?.let {
                        add("precondition(Self.self != ${scopeTypeName ?: it.swiftFqName}.self, \"${it.swiftFqName} is an abstract class and cannot be instantiated directly\")")
                    }
                }

                when {
                    isAbstractClass -> add("let ${obj.name} = _kotlinAllocInstanceForSwiftSubclass(Self.self)")
                    isInheritableClass && scope != null -> {
                        val allocDescriptor = bridgeAllocProxy ?: return@withSessions null
                        add("""
                        | let ${obj.name}: Swift.UnsafeMutableRawPointer!
                        | if Self.self == ${scopeTypeName ?: scope.swiftFqName}.self {
                        | ${allocDescriptor.createSwiftInvocation { "${obj.name} = $it" }.joinToString("\n|").prependIndent()}
                        | } else {
                        |     ${obj.name} = _kotlinAllocInstanceForSwiftSubclass(Self.self)
                        | }
                        """.trimMargin())
                    }
                    else -> {
                        val allocDescriptor = bridgeAllocProxy ?: return@withSessions null
                        addAll(allocDescriptor.createSwiftInvocation { "let ${obj.name} = $it" })
                    }
                }

                val superInitOptions = when {
                    !isGenericClass -> ".asBoundBridge"
                    isAbstractClass -> ".asBoundBridge"
                    isInheritableClass && scopeTypeName != null -> "Self.self == $scopeTypeName.self ? .asExistentialWrapper : .asBoundBridge"
                    else -> ".asExistentialWrapper"
                }
                add("super.init(__externalRCRefUnsafe: ${obj.name}, options: $superInitOptions);")

                addAll(initDescriptor.createSwiftInvocation(resultTransformer = null))
            })
        }

    private val bridgeAllocProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (!isBridged || bridgeInitProxy == null || isAbstractClass) return@lazyWithSessions null

        val fqName = ktSymbol.containingClassId?.asSingleFqName()
            ?: return@lazyWithSessions null

        val suffix = "_init" + "_allocate"

        val baseName = fqName.baseBridgeName + suffix

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = emptyList(),
            returnType = obj.type,
            kotlinFqName = fqName,
            kotlinOptIns = ktSymbol.containingDeclaration?.allRequiredOptIns ?: emptyList(),
            selfParameter = null,
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = null,
            isAsync = false,
            isInlinable = canRequestInlinable,
        )
    }

    private val bridgeInitProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (!isBridged) return@lazyWithSessions null
        if (isSelfUnavailable) return@lazyWithSessions null

        val fqName = ktSymbol.containingClassId?.asSingleFqName()
            ?: return@lazyWithSessions null

        val suffix = "_init" + "_initialize"

        val baseName = fqName.baseBridgeName + suffix

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOf(obj) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            kotlinOptIns = ktSymbol.allRequiredOptIns,
            selfParameter = null,
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter(null, "__error", it)
            },
            isAsync = false,
            isInlinable = canRequestInlinable,
        )
    }
}
