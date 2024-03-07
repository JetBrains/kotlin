/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildEnum
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.kt.*
import org.jetbrains.kotlin.sir.kt.nodes.SirClassFromSymbol
import org.jetbrains.kotlin.sir.kt.nodes.SirFunctionFromSymbol
import org.jetbrains.kotlin.sir.kt.nodes.SirNominalTypeFromSymbol
import org.jetbrains.kotlin.sir.kt.nodes.SirVariableFromSymbol
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

internal class SirDeclarationNamerImpl(
    private val layoutStrategy: SirDeclarationLayoutStrategy,
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirDeclarationNamer {
    override fun KtDeclarationSymbol.sirDeclarationName(): String {
        val isTopLevel = with(ktAnalysisSession) { this@sirDeclarationName.getContainingSymbol() } == null
        val declarationName = this.getName() ?: error("AAAA")
        return if (isTopLevel && layoutStrategy.collapsePackagesIntoTopLevelNames()) {
            val packagePrefix = this.getDeclarationPackage()?.collapse() ?: ""
            packagePrefix + declarationName.capitalizeAsciiOnly()
        } else {
            declarationName
        }
    }

    private fun KtDeclarationSymbol.getDeclarationPackage(): FqName? {
        return when (this) {
            is KtNamedClassOrObjectSymbol -> this.classIdIfNonLocal?.packageFqName
            is KtFunctionLikeSymbol -> this.callableIdIfNonLocal?.packageName
            is KtVariableSymbol -> this.callableIdIfNonLocal?.packageName
            else -> error(this)
        }
    }

    private fun KtDeclarationSymbol.getName(): String? {
        return when (this) {
            is KtNamedClassOrObjectSymbol -> this.classIdIfNonLocal?.shortClassName
            is KtFunctionLikeSymbol -> this.callableIdIfNonLocal?.callableName
            is KtVariableSymbol -> this.callableIdIfNonLocal?.callableName
            else -> error(this)
        }?.asString()
    }

    private fun FqName.collapse(): String =
        this.pathSegments().map { it.toString().capitalizeAsciiOnly() }.joinToString(separator = "")
}

internal class SirModuleProviderImpl(predefinedModules: Map<KtModule, SirModule> = emptyMap()) : SirModuleProvider {

    private val moduleCache = mutableMapOf<KtModule, SirModule>()

    init {
        moduleCache.putAll(predefinedModules)
    }

    override fun KtModule.sirModule(): SirModule {
        return moduleCache.getOrPut(this) {
            buildModule {
                this.name = this@sirModule.moduleName()
            }
        }
    }

    // TODO: Postprocess to make sure that module name is a correct Swift name
    private fun KtModule.moduleName(): String {
        return when (this) {
            is KtSourceModule -> this.moduleName
            is KtSdkModule -> this.sdkName
            is KtLibraryModule -> this.libraryName
            else -> error(this)
        }
    }
}

internal class SirDeclarationProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirDeclarationProvider {

    // TODO: We can switch here to a more sophisticated caching system here later.
    private val classCache = mutableMapOf<ClassId, SirClass>()
    private val functionCache = mutableMapOf<KtFunctionLikeSymbol, SirFunction>()
    private val propertyCache = mutableMapOf<KtKotlinPropertySymbol, SirVariable>()

    override fun KtDeclarationSymbol.sirDeclaration(): SirDeclaration {
        val ktSymbol = this@sirDeclaration
        return when (ktSymbol) {
            is KtNamedClassOrObjectSymbol -> classCache.getOrPut(ktSymbol.classIdIfNonLocal!!) {
                SirClassFromSymbol(
                    ktSymbol,
                    ktAnalysisSession,
                    sirSession
                )
            }
            is KtFunctionLikeSymbol -> functionCache.getOrPut(ktSymbol) { SirFunctionFromSymbol(ktSymbol, ktAnalysisSession, sirSession) }
            is KtKotlinPropertySymbol -> propertyCache.getOrPut(ktSymbol) { SirVariableFromSymbol(ktSymbol, ktAnalysisSession, sirSession) }
            else -> TODO("$ktSymbol")
        }
    }
}

// TODO: Handle different modules
internal class SirEnumGeneratorImpl() : SirEnumGenerator {

    private val enumCache = mutableMapOf<FqName, SirEnum>()
    override fun FqName.sirPackageEnum(module: SirModule): SirEnum {
        require(!this.isRoot)
        if (this.parent().isRoot) {
            return enumCache.getOrPut(this) {
                createEnum(this@sirPackageEnum).also {
                    it.parent = module
                    (module.declarations as MutableList<SirDeclaration>) += it
                }
            }
        } else {
            val parent = this.parent().sirPackageEnum(module)
            return enumCache.getOrPut(this) {
                createEnum(this@sirPackageEnum).also {
                    it.parent = parent
                    (parent.declarations as MutableList<SirDeclaration>) += it
                }
            }
        }
    }

    private fun createEnum(fqName: FqName): SirEnum {
        return buildEnum {
            origin = SirOrigin.Namespace(fqName.pathSegments().map { it.asString() })
            name = fqName.pathSegments().last().asString()
        }
    }
}

internal class SirParentProviderImpl(
    private val layoutStrategy: SirDeclarationLayoutStrategy,
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirParentProvider {
    override fun KtDeclarationSymbol.sirDeclarationParent(): SirDeclarationParent {
        val ktDeclarationSymbol = this@sirDeclarationParent
        with(ktAnalysisSession) {
            val containingSymbol = ktDeclarationSymbol.getContainingSymbol()
            // The declaration is top-level.
            if (containingSymbol == null) {
                val packageFqName = when (ktDeclarationSymbol) {
                    is KtNamedClassOrObjectSymbol -> ktDeclarationSymbol.classIdIfNonLocal?.packageFqName
                    is KtCallableSymbol -> ktDeclarationSymbol.callableIdIfNonLocal?.packageName
                    else -> null
                } ?: error("TODO")
                // Top-level declaration without module -> store directly in the module
                // OR
                // We don't emulate packages with enums -> store directly in the module
                if (packageFqName.isRoot || !layoutStrategy.generateEnumsForNamespaces()) {
                    return with(sirSession) {
                        ktDeclarationSymbol.getContainingModule().sirModule()
                    }
                } else {
                    // Use synthetic enums for packages
                    return with(sirSession) {
                        packageFqName.sirPackageEnum(ktDeclarationSymbol.getContainingModule().sirModule())
                    }
                }
            } else {
                // Declaration is nested in another declaration -> reflect it in the translation
                return with(sirSession) {
                    containingSymbol.sirDeclaration() as? SirDeclarationParent
                        ?: error("NOT A PARENT T_T")
                }
            }
        }

    }
}

internal class SirTypeProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirTypeProvider {
    override fun KtType.translateType(): SirType {
        with(ktAnalysisSession) {
            with(sirSession) {
                return buildSirNominalType(this@translateType)
            }
        }
    }

    context(KtAnalysisSession, SirSession)
    private fun buildSirNominalType(it: KtType): SirType {
        when {
            it.isUnit -> SirSwiftModule.void

            it.isByte -> SirSwiftModule.int8
            it.isShort -> SirSwiftModule.int16
            it.isInt -> SirSwiftModule.int32
            it.isLong -> SirSwiftModule.int64

            it.isUByte -> SirSwiftModule.uint8
            it.isUShort -> SirSwiftModule.uint16
            it.isUInt -> SirSwiftModule.uint32
            it.isULong -> SirSwiftModule.uint64

            it.isBoolean -> SirSwiftModule.bool

            it.isDouble -> SirSwiftModule.double
            it.isFloat -> SirSwiftModule.float
            else -> null
        }?.let {
            return SirPredefinedNominalType(it)
        }
        if (it.isDenotable) {
            val ktDeclaration = it.expandedClassSymbol!!
            return SirNominalTypeFromSymbol(analysisSession, sirSession, ktDeclaration)
        }
        error("UNSUPPORTED TYPE ${it.asStringForDebugging()}")
    }
}

internal class SirVisibilityCheckerImpl(private val ktAnalysisSession: KtAnalysisSession) : SirVisibilityChecker {
    override fun KtSymbolWithVisibility.sirVisibility(): SirVisibility? = with (ktAnalysisSession) {
        if (this@sirVisibility is KtNamedClassOrObjectSymbol && !this@sirVisibility.isConsumableBySirBuilder()) return null
        if (visibility.isPublicAPI) return SirVisibility.PUBLIC
        else return null
    }

    context(KtAnalysisSession)
    private fun KtNamedClassOrObjectSymbol.isConsumableBySirBuilder(): Boolean =
        classKind == KtClassKind.CLASS
                && (superTypes.count() == 1 && superTypes.first().isAny) // Every class has Any as a superclass
                && !isData
                && !isInline
                && modality == Modality.FINAL


    context(KtAnalysisSession)
    private fun KtFunctionLikeSymbol.isConsumableBySir(): Boolean {
        return true
    }
}