/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.containingSymbol
import org.jetbrains.kotlin.analysis.api.components.defaultType
import org.jetbrains.kotlin.analysis.api.components.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.source.InnerInitSource
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.translateType
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.returnType
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.OverrideStatus
import org.jetbrains.sir.lightclasses.utils.computeIsOverride
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import kotlin.lazy

private val obj = SirParameter("", "__kt",SirNominalType(SirSwiftModule.unsafeMutableRawPointer))

internal class SirInitFromKtSymbol(
    override val ktSymbol: KaConstructorSymbol,
    override val sirSession: SirSession,
) : SirInit(), SirFromKtSymbol<KaConstructorSymbol> {

    override val visibility: SirVisibility by lazyWithSessions {
        ktSymbol.sirAvailability().visibility ?: error("$ktSymbol shouldn't be exposed to SIR")
    }

    override val isFailable: Boolean = false

    override val origin: SirOrigin by lazyWithSessions {
        if (isInner(ktSymbol)) InnerInitSource(ktSymbol) else KotlinSource(ktSymbol)
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters() + listOfNotNull(getOuterParameterOfInnerClass())
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
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

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never

    private val isBridged: Boolean get() = withSessions {
            (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>()?.let {
                !it.modality.isAbstract() && !it.defaultType.isArrayOrPrimitiveArray
            } ?: false
        }

    override val bridges: List<SirBridge> by lazy {
        val producingType: SirType = SirNominalType(
            parent as? SirNamedDeclaration ?: error("Encountered an Init that produces non-named type: $parent")
        )

        listOfNotNull(
            bridgeAllocProxy?.createSirBridge {
                val args = argNames
                "kotlin.native.internal.createUninitializedInstance<${
                    typeNamer.kotlinFqName(
                        producingType,
                        SirTypeNamer.KotlinNameType.PARAMETRIZED
                    )
                }>(${args.joinToString()})"
            },
            if (origin is InnerInitSource) {
                bridgeInitProxy?.createSirBridge {
                    val args = this.argNames
                    require(kotlinFqName.size >= 2) {
                        "Expected >=2 kotlinFqName.size, but were ${kotlinFqName.size}: ${kotlinFqName.joinToString(",")}"
                    }
                    require(args.size >= 2) {
                        "Expected >=2 inner constructor arguments, but were ${args.size}: ${args.joinToString(",")}"
                    }
                    val outerClassName = kotlinFqName.dropLast(1).joinToString(".")
                    val innerClassName = kotlinFqName.last()
                    val innerConstructorArgs = args.drop(1).dropLast(1).joinToString(", ")
                    val innerConstructorCall = "(${args.last()} as $outerClassName).$innerClassName($innerConstructorArgs)"

                    "kotlin.native.internal.initInstance(${args.first()}, $innerConstructorCall)"
                }
            } else {
                bridgeInitProxy?.createSirBridge {
                    val args = argNames
                    "kotlin.native.internal.initInstance(${args.first()}, ${
                        typeNamer.kotlinFqName(
                            producingType,
                            SirTypeNamer.KotlinNameType.PARAMETRIZED
                        )
                    }(${args.drop(1).joinToString()}))"
                }
            },
        )
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() {
            val initDescriptor = bridgeInitProxy ?: return null
            val allocDescriptor = bridgeAllocProxy ?: return null

            return SirFunctionBody(buildList {
                (parent as? SirNamedDeclaration)?.let { it ->
                    add("if Self.self != ${it.swiftFqName}.self { fatalError(\"Inheritance from exported Kotlin classes is not supported yet: \\(String(reflecting: Self.self)) inherits from ${it.swiftFqName} \") }")
                }

                addAll(allocDescriptor.createSwiftInvocation {
                    "let ${obj.name} = $it"
                })

                add("super.init(__externalRCRefUnsafe: ${obj.name}, options: .asBoundBridge)")

                addAll(initDescriptor.createSwiftInvocation(resultTransformer = null))
            })
        }

    private val bridgeAllocProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (!isBridged || bridgeInitProxy == null) return@lazyWithSessions null

        val fqName = ktSymbol.containingClassId?.asSingleFqName()
            ?.pathSegments()?.map { it.toString() }
            ?: return@lazyWithSessions null

        val suffix = "_init" + "_allocate"

        val baseName = fqName.forBridge.joinToString("_") + suffix

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = emptyList(),
            returnType = obj.type,
            kotlinFqName = fqName,
            selfParameter = null,
            extensionReceiverParameter = null,
            errorParameter = null,
        )
    }

    private val bridgeInitProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (!isBridged) return@lazyWithSessions null

        val fqName = ktSymbol.containingClassId?.asSingleFqName()
            ?.pathSegments()?.map { it.toString() }
            ?: return@lazyWithSessions null

        val suffix = "_init" + "_initialize"

        val baseName = fqName.forBridge.joinToString("_") + suffix

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOf(obj) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            selfParameter = null,
            extensionReceiverParameter = null,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "__error", it)
            },
        )
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