/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import kotlin.lazy

internal abstract class SirAbstractVariableFromKtSymbol(
    override val ktSymbol: KaVariableSymbol,
    override val sirSession: SirSession,
) : SirVariable(), SirFromKtSymbol<KaVariableSymbol> {
    private class DefaultGetter(
        override val ktSymbol: KaVariableSymbol,
        sirSession: SirSession,
    ) : SirAbstractGetter(sirSession), SirFromKtSymbol<KaVariableSymbol> {
        override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
        override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
        override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
    }

    private class DefaultSetter(
        override val ktSymbol: KaVariableSymbol,
        sirSession: SirSession,
    ) : SirAbstractSetter(sirSession), SirFromKtSymbol<KaVariableSymbol> {
        override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
        override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
        override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
    }

    override val visibility: SirVisibility = SirVisibility.PUBLIC

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
    }
    override val type: SirType by lazy {
        translateReturnType()
    }
    override val getter: SirGetter by lazy {
        ((ktSymbol as? KaPropertySymbol)?.let {
            it.getter?.let {
                SirGetterFromKtSymbol(it, sirSession)
            }
        } ?: DefaultGetter(ktSymbol, sirSession)).also {
            it.parent = this@SirAbstractVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? by lazy {
        (ktSymbol as? KaPropertySymbol)
            ?.takeIf { it.setter?.visibility == KaSymbolVisibility.PUBLIC }
            ?.let {
                it.setter?.let { SirSetterFromKtSymbol(it, sirSession) }
                    ?: if (!it.isVal) DefaultSetter(it, sirSession) else null
            }
            ?.apply { parent = this@SirAbstractVariableFromKtSymbol }
    }
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val isOverride: Boolean
        get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirVariable>? by lazy { computeIsOverride() }

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality

    override val bridges: List<SirBridge> = emptyList()
}

internal class SirVariableFromKtSymbol(
    ktSymbol: KaVariableSymbol,
    sirSession: SirSession,
) : SirAbstractVariableFromKtSymbol(ktSymbol, sirSession) {
    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel && !(ktSymbol is KaPropertySymbol && ktSymbol.isStatic)
}

internal abstract class SirAbstractGetter(
    val sirSession: SirSession,
) : SirGetter() {
    override lateinit var parent: SirDeclarationParent
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? get() = null
    override val attributes: List<SirAttribute> get() = emptyList()
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean get() = false
    private val variable get() = parent as? SirVariable

    open val fqName: FqName? by lazyWithSessions {
        variable?.kaSymbolOrNull<KaVariableSymbol>()
            ?.callableId?.asSingleFqName()
    }

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val suffix = "_get"
        val variable = variable ?: return@lazyWithSessions null
        val fqName = fqName ?: return@lazyWithSessions null
        val baseName = fqName.baseBridgeName + suffix

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = emptyList(),
            returnType = variable.type,
            kotlinFqName = fqName,
            selfParameter = (variable.parent !is SirModule && variable.isInstance).ifTrue {
                SirParameter("", "self", selfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "_out_error", it)
            },
            isAsync = false,
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        bridgeProxy?.createSirBridges {
            val args = argNames
            val expectedParameters = if (extensionReceiverParameter != null) 1 else 0
            require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
            buildCall("")
        }.orEmpty()
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = bridgeProxy?.createSwiftInvocation { "return $it" }?.let(::SirFunctionBody)

    private inline fun <R> lazyWithSessions(crossinline block: context(KaSession, SirSession) () -> R): Lazy<R> = lazy {
        sirSession.withSessions(block)
    }
}

internal class SirGetterFromKtSymbol(
    override val ktSymbol: KaPropertyGetterSymbol,
    sirSession: SirSession,
) : SirAbstractGetter(sirSession), SirFromKtSymbol<KaPropertyGetterSymbol> {
    override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
    override val documentation: String? by lazy { ktSymbol.documentation() }
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
}

internal abstract class SirAbstractSetter(
    val sirSession: SirSession,
) : SirSetter(), SirBridgedCallable {
    override lateinit var parent: SirDeclarationParent
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? get() = null
    override val parameterName: String = "newValue"
    override val attributes: List<SirAttribute> get() = emptyList()
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean get() = false
    private val variable get() = parent as? SirVariable

    open val fqName: FqName? by lazyWithSessions {
        variable?.kaSymbolOrNull<KaVariableSymbol>()
            ?.callableId?.asSingleFqName()
    }

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val suffix = "_set"
        val variable = variable ?: return@lazyWithSessions null
        val fqName = fqName ?: return@lazyWithSessions null
        val baseName = fqName.baseBridgeName + suffix

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOf(SirParameter(parameterName = parameterName, type = variable.type)),
            returnType = SirNominalType(SirSwiftModule.void),
            kotlinFqName = fqName,
            selfParameter = (parent !is SirModule && variable.isInstance).ifTrue {
                SirParameter("", "self", selfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "_out_error", it)
            },
            isAsync = false,
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        bridgeProxy?.createSirBridges {
            val args = argNames
            val expectedParameters = if (extensionReceiverParameter != null) 2 else 1
            require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
            buildCall(" = ${args.last()}")
        }.orEmpty()
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = bridgeProxy?.createSwiftInvocation { "return $it" }?.let(::SirFunctionBody)

    private inline fun <R> lazyWithSessions(crossinline block: context(KaSession, SirSession) () -> R): Lazy<R> = lazy {
        sirSession.withSessions(block)
    }
}

internal class SirSetterFromKtSymbol(
    override val ktSymbol: KaPropertySetterSymbol,
    sirSession: SirSession,
) : SirAbstractSetter(sirSession), SirFromKtSymbol<KaPropertySetterSymbol> {
    override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
    override val documentation: String? by lazy { ktSymbol.documentation() }
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
}
