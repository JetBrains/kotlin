/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.translation

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.klib.reader.getAllClassifiers
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.SirKaClassReferenceHandler
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.allParameters
import org.jetbrains.kotlin.sir.util.conflictsWith
import org.jetbrains.kotlin.sir.util.returnType
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportLogger
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.builders.KaModules
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSirSession
import org.jetbrains.kotlin.swiftexport.standalone.builders.translateModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.writer.BridgeSources
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.sir.printer.SirPrinter

/**
 * Translates the whole public API surface of the given [module] to [SirModule] and generates compiler bridges between them.
 */
internal fun translateModulePublicApi(module: InputModule, kaModules: KaModules, config: SwiftExportConfig): TranslationResult {
    // We access KaSymbols through all the module translation process. Since it is not correct to access them directly
    // outside of the session they were created, we create KaSession here.
    return analyze(kaModules.useSiteModule) {
        val externalTypeDeclarationReferences = mutableMapOf<KaLibraryModule, MutableList<FqName>>()
        val externalTypeReferenceHandler = SirKaClassReferenceHandler { symbol ->
            val symbolContainingModule = symbol.containingModule as? KaLibraryModule
            symbolContainingModule?.let { libraryName ->
                externalTypeDeclarationReferences
                    .getOrPut(libraryName) { mutableListOf() }
                    .addIfNotNull(symbol.classId?.asSingleFqName())
            }
        }
        buildSirSession(module.name, kaModules, config, module.config, externalTypeReferenceHandler).withSessions {
            val sirModule = translateModule(
                module = kaModules.mainModules.single { it.libraryName == module.name }
            )
            createTranslationResult(sirModule, config, module.config, externalTypeDeclarationReferences)
        }
    }
}

/**
 * Accumulates module translation state during transitive export.
 */
private class ModuleTransitiveTranslationState(
    val kaModule: KaLibraryModule,
    val moduleConfig: SwiftModuleConfig,
    val unprocessedReferences: MutableSet<FqName>,
    var currentlyProcessing: List<FqName>,
    val processedReferences: MutableSet<FqName>,
) {
    lateinit var sirSession: SirSession
}

/**
 * Translate a group of cross-referencing [transitivelyExportedModules] into Swift.
 * Unlike [translateModulePublicApi], this function translates only referenced type declarations, leaving
 * top-level functions, properties, and unreferenced type declarations untouched.
 * [typeDeclarationReferences] acts as a root for the translation process.
 *
 * @return a list of non-empty [TranslationResult].
 */
internal fun translateCrossReferencingModulesTransitively(
    typeDeclarationReferences: Map<KaLibraryModule, List<FqName>>,
    kaModules: KaModules,
    config: SwiftExportConfig,
): List<TranslationResult> {
    val translationStates = typeDeclarationReferences
        .map { (module, references) ->
            ModuleTransitiveTranslationState(
                kaModule = module,
                moduleConfig = kaModules.configFor(module),
                unprocessedReferences = references.toMutableSet(),
                currentlyProcessing = emptyList(),
                processedReferences = mutableSetOf(),
            )
        }
    val typeReferenceHandler = SirKaClassReferenceHandler { symbol ->
        analyze(kaModules.useSiteModule) {
            val libraryName = (symbol.containingModule as? KaLibraryModule)?.libraryName
            translationStates.find { it.kaModule.libraryName == libraryName }?.let {
                val fqName = symbol.classId?.asSingleFqName()
                    ?: return@analyze
                if (fqName !in it.processedReferences && fqName !in it.currentlyProcessing) {
                    it.unprocessedReferences += fqName
                }
            }
        }
    }
    translationStates.forEach {
        it.sirSession = buildSirSession(it.kaModule.libraryName, kaModules, config, it.moduleConfig, typeReferenceHandler)
    }
    // Translate modules until the process converges.
    while (translationStates.any { it.unprocessedReferences.isNotEmpty() }) {
        translationStates
            .filter { it.unprocessedReferences.isNotEmpty() }
            .map {
                // Copy new references to avoid concurrent modification exception.
                it.currentlyProcessing = it.unprocessedReferences.toList()
                it.unprocessedReferences.clear()
                val sirModule = it.sirSession.withSessions {
                    translateModule(module = it.kaModule) { module ->
                        module.getAllClassifiers()
                            .filterIsInstance<KaClassLikeSymbol>()
                            .filter { symbol -> symbol.classId?.asSingleFqName() in it.currentlyProcessing }
                    }
                }
                it.processedReferences += it.currentlyProcessing
                it.currentlyProcessing = emptyList()
                // Touch all declarations
                it.sirSession.withSessions {
                    deepTouch(sirModule, typeReferenceHandler::onClassReference)
                }
            }
    }
    return translationStates.mapNotNull {
        with(it.sirSession) {
            val sirModule = it.kaModule.sirModule()
            // Avoid generation of empty modules.
            if (sirModule.declarations.isEmpty()) return@mapNotNull null
            createTranslationResult(
                sirModule,
                config,
                it.moduleConfig,
                emptyMap(),
            )
        }
    }
}

context(sir: SirSession)
private fun createTranslationResult(
    sirModule: SirModule,
    config: SwiftExportConfig,
    moduleConfig: SwiftModuleConfig,
    externalTypeDeclarationReferences: Map<KaLibraryModule, List<FqName>>,
): TranslationResult {
    // Assume that parts of the KotlinRuntimeSupport and KotlinRuntime module are used.
    // It might not be the case, but precise tracking seems like an overkill at the moment.
    sirModule.updateImport(SirImport(config.runtimeSupportModuleName))
    sirModule.updateImport(SirImport(config.runtimeModuleName))

    // Conflicts may have arisen from the package flattening process
    sirModule.removeConflicts(config.logger)

    val bridgeModuleName = "${moduleConfig.bridgeModuleName}_${sirModule.name}"

    val printer = SirPrinter(
        config.stableDeclarationsOrder,
        config.renderDocComments,
    )
    val bridgeSources = generateModuleBridges(printer, sirModule, bridgeModuleName)
    // Serialize SirModule to sources to avoid leakage of SirSession (and KaSession, likely) outside the analyze call.
    val swiftSourceCode = printer.print(sirModule).swiftSource.joinToString("\n")

    val knownModuleNames = setOf(KotlinRuntimeModule.name, bridgeModuleName) + config.platformLibsInputModule.map { it.name }
    val referencedSwiftModules = sirModule.imports
        .filter { it.moduleName !in knownModuleNames }
        .map { SwiftExportModule.Reference(it.moduleName) }
    return TranslationResult(
        swiftModuleName = sirModule.name,
        swiftModuleSources = swiftSourceCode,
        referencedSwiftModules = referencedSwiftModules,
        packages = sir.enumGenerator.collectedPackages,
        bridgeSources = bridgeSources,
        moduleConfig = moduleConfig,
        bridgesModuleName = bridgeModuleName,
        externalTypeDeclarationReferences = externalTypeDeclarationReferences,
    )
}

/**
 * Generates method bodies for functions in [sirModule], as well as Kotlin and C [BridgeSources].
 */
private fun generateModuleBridges(printer: SirPrinter, sirModule: SirModule, bridgeModuleName: String): BridgeSources {
    val printout = printer.print(sirModule)
    val cSources = printout.cSource
    val kotlinSources = printout.kotlinSource

    if (printout.hasBridges) {
        sirModule.updateImport(
            SirImport(
                moduleName = bridgeModuleName,
                mode = SirImport.Mode.ImplementationOnly
            )
        )
    }

    return BridgeSources(ktSrc = kotlinSources, cSrc = cSources)
}

internal class TranslationResult(
    val swiftModuleName: String,
    val swiftModuleSources: String,
    val referencedSwiftModules: List<SwiftExportModule.Reference>,
    val packages: Set<FqName>,
    val bridgeSources: BridgeSources,
    val moduleConfig: SwiftModuleConfig,
    val bridgesModuleName: String,
    val externalTypeDeclarationReferences: Map<KaLibraryModule, List<FqName>>,
)

private fun deepTouch(
    container: SirDeclarationContainer,
    symbolHandler: (KaClassLikeSymbol) -> Unit = {},
): Unit = with(container.declarations.toList()) {
    // This invokes SirKaClassReferenceHandler under the hood for Kotlin-exported types.
    if (container is SirProtocolConformingDeclaration) {
        container.protocols
    }
    if (container is SirClassInhertingDeclaration) {
        container.superClass
    }

    filterIsInstance<SirCallable>().forEach {
        it.allParameters
        it.returnType
    }

    filterIsInstance<SirVariable>().forEach {
        it.type
    }

    filterIsInstance<SirTypealias>().forEach {
        it.type
    }

    filterIsInstance<SirDeclarationContainer>().forEach {
        deepTouch(it)
    }

    // Ensure each newly occuring declaration gets included in `unprocessedReferences`
    container.declarations.toList().takeIf { it != this }?.let {
        (it - this).forEach { it.kaSymbolOrNull<KaClassLikeSymbol>()?.let(symbolHandler) }
    }
}

private fun SirMutableDeclarationContainer.removeConflicts(logger: SwiftExportLogger?) {
    val trashBin = buildModule { name = "removedDueToConflicts" }
    val iterator = this.declarations.listIterator()
    while (iterator.hasNext()) {
        val decl = iterator.next()
        declarations
            .indexOfFirst { decl.conflictsWith(it) } // We assume that `conflictsWith` is transitive
            .takeIf { it in 0..<iterator.previousIndex() }
            ?.let { i ->
                val expelled = decl.takeIf { decl.priority < declarations[i].priority } ?: declarations[i].also { declarations[i] = decl }
                iterator.remove()
                expelled.parent = trashBin
                logger?.report(
                    SwiftExportLogger.Severity.Warning,
                    "Exported declaration $expelled was removed from export due to conflicts")
            }
    }
}

private val SirDeclaration.priority: Int get() = when (this) {
        is SirVariable -> 10
        is SirFunction -> 20
        is SirScopeDefiningDeclaration -> 30
        else -> 0
    }.let {
        if (this.origin is SirOrigin.Trampoline)
            it - 50
        else
            it
    }
