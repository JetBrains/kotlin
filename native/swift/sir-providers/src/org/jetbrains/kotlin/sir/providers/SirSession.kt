/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.impl.SirTypeProviderImpl.TypeTranslationCtx
import org.jetbrains.kotlin.sir.providers.impl.StandaloneSirTypeNamer


/**
 * A single entry point for all facilities that are required for running Analysis API -> SIR translation.
 *
 * Similar classes:
 * 1. [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] from Analysis API.
 * 2. [FirSession][org.jetbrains.kotlin.fir.FirSession] from K2.
 */
public interface SirSession :
    SirDeclarationNamer,
    SirDeclarationProvider,
    SirParentProvider,
    SirTrampolineDeclarationsProvider,
    SirModuleProvider,
    SirTypeProvider,
    SirVisibilityChecker,
    SirChildrenProvider,
    SirBridgeProvider,
    SirCustomTypeTranslator
{
    public val sirSession: SirSession
        get() = this

    public val useSiteModule: KaModule
    public val moduleToTranslate: KaModule

    public val enumGenerator: SirEnumGenerator

    public val declarationNamer: SirDeclarationNamer
    public val declarationProvider: SirDeclarationProvider
    public val parentProvider: SirParentProvider
    public val trampolineDeclarationsProvider: SirTrampolineDeclarationsProvider
    public val moduleProvider: SirModuleProvider
    public val typeProvider: SirTypeProvider
    public val customTypeTranslator: SirCustomTypeTranslator
    public val visibilityChecker: SirVisibilityChecker
    public val childrenProvider: SirChildrenProvider
    public val bridgeProvider: SirBridgeProvider

    override val errorTypeStrategy: SirTypeProvider.ErrorTypeStrategy
        get() = typeProvider.errorTypeStrategy
    override val unsupportedTypeStrategy: SirTypeProvider.ErrorTypeStrategy
        get() = typeProvider.unsupportedTypeStrategy

    override fun KaDeclarationSymbol.sirDeclarationName(): String = with(declarationNamer) { this@sirDeclarationName.sirDeclarationName() }

    override fun KaDeclarationSymbol.toSir(): SirTranslationResult = with(declarationProvider) { this@toSir.toSir() }

    override fun KaDeclarationSymbol.getSirParent(): SirDeclarationParent =
        with(parentProvider) { this@getSirParent.getSirParent() }

    override fun KaDeclarationSymbol.getOriginalSirParent(): SirElement =
        with(parentProvider) { this@getOriginalSirParent.getOriginalSirParent() }

    override fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration> = with (trampolineDeclarationsProvider) {
        this@trampolineDeclarations.trampolineDeclarations()
    }

    override fun KaModule.sirModule(): SirModule = with(moduleProvider) { this@sirModule.sirModule() }

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        position: SirTypeVariance,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
        requiresHashableAsAny: Boolean,
    ): SirType =
        with(typeProvider) {
            this@translateType.translateType(
                ktAnalysisSession,
                position,
                reportErrorType,
                reportUnsupportedType,
                processTypeImports,
                requiresHashableAsAny,
            )
        }

    @Deprecated("Use this.sirAvailability instead", ReplaceWith("this.sirAvailability(ktAnalysisSession)"))
    @Suppress("DEPRECATION")
    override fun KaDeclarationSymbol.sirVisibility(ktAnalysisSession: KaSession): SirVisibility? =
        with(visibilityChecker) { this@sirVisibility.sirVisibility(ktAnalysisSession) }

    override fun KaDeclarationSymbol.sirAvailability(): SirAvailability =
        with(visibilityChecker) { this@sirAvailability.sirAvailability() }

    override fun Sequence<KaDeclarationSymbol>.extractDeclarations(): Sequence<SirDeclaration> =
        with(childrenProvider) { this@extractDeclarations.extractDeclarations() }

    override fun isFqNameSupported(fqName: FqName): Boolean =
        with(customTypeTranslator) { isFqNameSupported(fqName) }

    public fun isClassIdSupported(classId: ClassId): Boolean = isFqNameSupported(classId.asSingleFqName())

    public fun isTypeSupported(type: KaType): Boolean = type is KaClassType && isClassIdSupported(type.classId)

    context(kaSession: KaSession)
    override fun KaUsualClassType.toSirTypeBridge(ctx: TypeTranslationCtx): SirCustomTypeTranslator.BridgeWrapper? =
        with(customTypeTranslator) { toSirTypeBridge(ctx) }

    override fun SirNominalType.toBridge(): SirCustomTypeTranslator.BridgeWrapper? =
        with(customTypeTranslator) { toBridge() }

    override fun generateFunctionBridge(
        baseBridgeName: String,
        explicitParameters: List<SirParameter>,
        returnType: SirType,
        kotlinFqName: FqName,
        selfParameter: SirParameter?,
        contextParameters: List<SirParameter>,
        extensionReceiverParameter: SirParameter?,
        errorParameter: SirParameter?,
        isAsync: Boolean,
    ): BridgeFunctionProxy? = with(bridgeProvider) {
        generateFunctionBridge(
            baseBridgeName,
            explicitParameters,
            returnType,
            kotlinFqName,
            selfParameter,
            contextParameters,
            extensionReceiverParameter,
            errorParameter,
            isAsync,
        )
    }

    override fun generateTypeBridge(
        kotlinFqName: FqName?,
        swiftFqName: String,
        swiftSymbolName: String,
    ): SirTypeBindingBridge? = with(bridgeProvider) {
        generateTypeBridge(kotlinFqName, swiftFqName, swiftSymbolName)
    }
}

/**
 * Provides methods to create [SirEnum] which emulates Kotlin packages.
 */
public interface SirEnumGenerator {
    public fun FqName.sirPackageEnum(): SirEnum
    public val collectedPackages: Set<FqName>
}

/**
 * Names SIR declarations that are constructed from [KaDeclarationSymbol].
 */
public interface SirDeclarationNamer {
    public fun KaDeclarationSymbol.sirDeclarationName(): String
}

context(sir: SirSession)
public fun KaDeclarationSymbol.sirDeclarationName(): String = with(sir) { sirDeclarationName() }

public sealed interface SirTranslationResult {
    public val allDeclarations: List<SirDeclaration>
    public val primaryDeclaration: SirDeclaration?

    public sealed interface TypeDeclaration : SirTranslationResult {
        public val declaration: SirScopeDefiningDeclaration
        override val primaryDeclaration: SirDeclaration? get() = declaration
    }

    public data class Untranslatable(public val origin: SirOrigin) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> get() = emptyList()
        override val primaryDeclaration: SirDeclaration? get() = null
    }

    public data class RegularClass(public override val declaration: SirClass) : TypeDeclaration {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
    }

    public data class TypeAlias(public override val declaration: SirTypealias) : TypeDeclaration {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
    }

    public data class Enum(public override val declaration: SirEnum) : TypeDeclaration {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
    }

    public data class EnumCase(public val declaration: SirEnumCase) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
        override val primaryDeclaration: SirDeclaration get() = declaration
    }

    public data class Constructor(public val declaration: SirInit) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
        override val primaryDeclaration: SirDeclaration get() = declaration
    }

    public data class RegularProperty(public val declaration: SirVariable) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
        override val primaryDeclaration: SirDeclaration get() = declaration

    }

    public data class ExtensionProperty(public val getter: SirFunction, public val setter: SirFunction?) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOfNotNull(getter, setter)
        override val primaryDeclaration: SirDeclaration get() = getter
    }

    public data class RegularFunction(public val declaration: SirFunction) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration)
        override val primaryDeclaration: SirDeclaration get() = declaration
    }

    public data class OperatorFunction(
        public val declaration: SirFunction,
        public val supplementaryDeclarations: List<SirDeclaration>
    ) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration) + supplementaryDeclarations
        override val primaryDeclaration: SirDeclaration get() = declaration
    }

    public data class OperatorSubscript(
        public val declaration: SirSubscript,
        public val supplementaryDeclarations: List<SirDeclaration>
    ) : SirTranslationResult {
        override val allDeclarations: List<SirDeclaration> = listOf(declaration) + supplementaryDeclarations
        override val primaryDeclaration: SirDeclaration get() = declaration
    }

    public data class RegularInterface(
        public val declaration: SirProtocol,
        public val bridgedImplementation: SirExtension?,
        public val markerDeclaration: SirProtocol,
        public val existentialExtension: SirExtension,
        public val auxExtension: SirExtension,
        public val samConverter: SirDeclaration?,
    ) : SirTranslationResult {
        override val primaryDeclaration: SirDeclaration get() = declaration
        override val allDeclarations: List<SirDeclaration> =
            listOfNotNull(
                declaration,
                bridgedImplementation,
                markerDeclaration,
                existentialExtension,
                auxExtension,
                samConverter,
            )
    }

    public data class StubClass(
        public val declaration: SirClass,
    ) : SirTranslationResult {
        override val primaryDeclaration: SirDeclaration get() = declaration
        override val allDeclarations: List<SirDeclaration> = listOfNotNull(declaration)
    }

    public data class StubInterface(
        public val declaration: SirProtocol,
    ) : SirTranslationResult {
        override val primaryDeclaration: SirDeclaration get() = declaration
        override val allDeclarations: List<SirDeclaration> = listOfNotNull(declaration)
    }
}

/**
 * A single entry point to create a lazy wrapper around the given [KaDeclarationSymbol].
 */
public interface SirDeclarationProvider {
    public fun KaDeclarationSymbol.toSir(): SirTranslationResult

    @Deprecated(
        "This is provided for compatibility with external code. Prefer structured result version",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toSIR().allDeclarations")
    )
    public fun KaDeclarationSymbol.sirDeclarations(): List<SirDeclaration> = toSir().allDeclarations
}

context(sir: SirSession)
public fun KaDeclarationSymbol.toSir(): SirTranslationResult = with(sir) { toSir() }

/**
 * Given [KaDeclarationSymbol] will produce [SirDeclarationParent], representing the parent for corresponding sir node.
 *
 * For example, given the top level function without a package - will return SirModule that should declare that declarations.
 * Or, given the top level function with a package - will return SirExtension for that package.
 *
 */
public interface SirParentProvider {
    public fun KaDeclarationSymbol.getSirParent(): SirDeclarationParent

    /**
     * Get original sir parent
     * Some bridged kotlin declaration is unsuitable for hosting other declarations in swift (e.g. protocols, packaged top-levels etc).
     * When that is the case, [SirParentProvider] attempts to relocate children declarations into the most appropriate place.
     * This method returns the original intended parent declaration that the receiver may have been relocated from.
     *
     * @return Sir element for original parent symbol. This is the same as [getSirParent] if the receiver was never relocated.
     */
    public fun KaDeclarationSymbol.getOriginalSirParent(): SirElement
}

context(sir: SirSession)
public fun KaDeclarationSymbol.getSirParent(): SirDeclarationParent = with(sir) { getSirParent() }

context(sir: SirSession)
public fun KaDeclarationSymbol.getOriginalSirParent(): SirElement = with(sir) { getOriginalSirParent() }

/**
 *  Provides trampoline declarations for a given [SirDeclaration], if any.
 */
public interface SirTrampolineDeclarationsProvider {
    public fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration>
}

context(sir: SirSession)
public fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration> = with(sir) { trampolineDeclarations() }

/**
 * Translates the given [KaModule] to the corresponding [SirModule].
 * Note that it is not always a 1-1 mapping.
 */
public interface SirModuleProvider {

    public fun KaModule.sirModule(): SirModule
}

context(sir: SirSession)
public fun KaModule.sirModule(): SirModule = with(sir) { sirModule() }

// TODO: SirChildrenProvider probably does not make much sense as a provider,
//  as it acts as a combination of several other provider (declaration, trampoline, visibility)
public interface SirChildrenProvider {

    public fun KaScope.extractDeclarations(): Sequence<SirDeclaration> =
        declarations.extractDeclarations()

    public fun Sequence<KaDeclarationSymbol>.extractDeclarations(): Sequence<SirDeclaration>
}

context(sir: SirSession)
public fun KaScope.extractDeclarations(): Sequence<SirDeclaration> = with(sir) { extractDeclarations() }

context(sir: SirSession)
public fun Sequence<KaDeclarationSymbol>.extractDeclarations(): Sequence<SirDeclaration> = with(sir) { extractDeclarations() }

public interface SirTypeProvider {

    public val errorTypeStrategy: ErrorTypeStrategy
    public val unsupportedTypeStrategy: ErrorTypeStrategy

    public enum class ErrorTypeStrategy {
        Fail, ErrorType
    }

    /**
     * Translates the given [KaType] to [SirType].
     * Calls [reportErrorType] / [reportUnsupportedType] if error/unsupported type
     * is encountered and [errorTypeStrategy] / [unsupportedTypeStrategy] instructs to fail.
     *
     * [processTypeImports] is called with the imports required to use the resulting type properly.
     */
    public fun KaType.translateType(
        ktAnalysisSession: KaSession,
        position: SirTypeVariance,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
        requiresHashableAsAny: Boolean = false,
    ): SirType
}

context(ka: KaSession, sir: SirSession)
public fun KaType.translateType(
    position: SirTypeVariance,
    reportErrorType: (String) -> Nothing,
    reportUnsupportedType: () -> Nothing,
    processTypeImports: (List<SirImport>) -> Unit,
): SirType = with(sir) { translateType(ka, position, reportErrorType, reportUnsupportedType, processTypeImports) }

context(kaSession: KaSession, sir: SirSession)
public fun KaUsualClassType.toSirTypeBridge(ctx: TypeTranslationCtx): SirCustomTypeTranslator.BridgeWrapper? =
    with(sir) { toSirTypeBridge(ctx) }

context(sir: SirSession)
public fun SirNominalType.toBridge(): SirCustomTypeTranslator.BridgeWrapper? =
    with(sir) { toBridge() }

public interface SirCustomTypeTranslator {
    public fun isFqNameSupported(fqName: FqName): Boolean

    context(kaSession: KaSession)
    public fun KaUsualClassType.toSirTypeBridge(ctx: TypeTranslationCtx): BridgeWrapper?

    public fun SirNominalType.toBridge(): BridgeWrapper?

    public class BridgeWrapper internal constructor(internal val bridge: Bridge)
}

/**
 * Generates a list of [SirBridge] for given [SirDeclaration].
 */
public interface SirBridgeProvider {
    public fun generateFunctionBridge(
        baseBridgeName: String,
        explicitParameters: List<SirParameter>,
        returnType: SirType,
        kotlinFqName: FqName,
        selfParameter: SirParameter?,
        contextParameters: List<SirParameter>,
        extensionReceiverParameter: SirParameter?,
        errorParameter: SirParameter?,
        isAsync: Boolean,
    ): BridgeFunctionProxy?

    public fun generateTypeBridge(
        kotlinFqName: FqName?,
        swiftFqName: String,
        swiftSymbolName: String,
    ): SirTypeBindingBridge?
}

context(sir: SirSession)
public fun generateFunctionBridge(
    baseBridgeName: String,
    explicitParameters: List<SirParameter>,
    returnType: SirType,
    kotlinFqName: FqName,
    selfParameter: SirParameter?,
    contextParameters: List<SirParameter>,
    extensionReceiverParameter: SirParameter?,
    errorParameter: SirParameter?,
    isAsync: Boolean,
): BridgeFunctionProxy? = with(sir) {
    generateFunctionBridge(
        baseBridgeName,
        explicitParameters,
        returnType,
        kotlinFqName,
        selfParameter,
        contextParameters,
        extensionReceiverParameter,
        errorParameter,
        isAsync,
    )
}

context(sir: SirSession)
public fun generateTypeBridge(
    kotlinFqName: FqName?,
    swiftFqName: String,
    swiftSymbolName: String,
): SirTypeBindingBridge? = with(sir) { generateTypeBridge(kotlinFqName, swiftFqName, swiftSymbolName) }

/**
 * Matches a [SirType] to its declaration name in either kotlin or swift.
 */
public interface SirTypeNamer {
    public enum class KotlinNameType {
        FQN, PARAMETRIZED
    }

    public fun swiftFqName(type: SirType): String
    public fun kotlinFqName(sirType: SirType, nameType: KotlinNameType): String
    public fun kotlinPrimitiveFqNameIfAny(sirType: SirType): String?
}

public fun SirTypeNamer(): SirTypeNamer = StandaloneSirTypeNamer

public interface SirVisibilityChecker {
    /**
     * Determines visibility of the given [KaDeclarationSymbol].
     * @return null if symbol should not be exposed to SIR completely.
     */
    @Deprecated(
        "Use sirAvailability instead",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.sirAvailability(ktAnalysisSession)")
    )
    public fun KaDeclarationSymbol.sirVisibility(ktAnalysisSession: KaSession): SirVisibility? =
        this.sirAvailability().visibility

    /**
     * Determines availability of the given [KaDeclarationSymbol].
     */
    public fun KaDeclarationSymbol.sirAvailability(): SirAvailability
}

context(sir: SirSession)
public fun KaDeclarationSymbol.sirAvailability(): SirAvailability = with(sir) { sirAvailability() }

public inline fun <T> SirSession.withSessions(crossinline block: context(KaSession, SirSession) () -> T): T {
    return analyze(this.useSiteModule) {
        block()
    }
}
