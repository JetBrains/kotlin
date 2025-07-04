/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.export.utilities.hasTypeParameter
import org.jetbrains.kotlin.analysis.api.export.utilities.isClone
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirAvailability
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.analysis.api.export.utilities.*
import org.jetbrains.kotlin.sir.providers.utils.isFromTemporarilyIgnoredPackage

public class SirVisibilityCheckerImpl(
    private val sirSession: SirSession,
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {
    @OptIn(KaExperimentalApi::class)
    override fun KaDeclarationSymbol.sirAvailability(ktAnalysisSession: KaSession): SirAvailability = sirSession.withSessions {
        val ktSymbol = this@sirAvailability

        val visibility = object {
            var value: SirVisibility = SirVisibility.entries.last()
                set(newValue) { field = minOf(field, newValue) }
        }

        val containingModule = ktSymbol.containingModule.sirModule()
        if (containingModule is SirPlatformModule) {
            // The majority of platform libraries can be mapped onto Xcode SDK modules. However, there are exceptions to this rule.
            // This means that `import $platformLibraryName` would be invalid in Swift. For the sake of simplicity, we skip such declarations.
            if (setOf("posix", "darwin", "zlib", "objc", "builtin", "CFCGTypes").contains(containingModule.name)) {
                return@withSessions SirAvailability.Unavailable("Types from certain platform libraries are hidden")
            } else {
                if (ktSymbol is KaTypeAliasSymbol)
                    return@withSessions SirAvailability.Hidden("Typealiases from platform libs are sometimes point to custom-exported objc types which we can not detect")
                else
                    visibility.value = SirVisibility.PUBLIC
            }
        }

        // We care only about public API.
        if (!ktSymbol.compilerVisibility.isPublicAPI) {
            visibility.value = SirVisibility.PRIVATE
        }
        // Hidden declarations are, well, hidden.
        if (ktSymbol.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN) {
            visibility.value = SirVisibility.PRIVATE
        }
        if (ktSymbol is KaCallableSymbol && ktSymbol.contextParameters.isNotEmpty()) {
            return@withSessions SirAvailability.Unavailable("Callables with context parameters are not supported yet")
        }
        if (containsHidesFromObjCAnnotation(ktSymbol)) {
            return@withSessions SirAvailability.Unavailable("Declaration is @HiddenFromObjC")
        }
        if ((ktSymbol.containingSymbol as? KaDeclarationSymbol?)?.sirAvailability(useSiteSession) is SirAvailability.Unavailable) {
            return@withSessions SirAvailability.Unavailable("Declaration's lexical parent is unavailable")
        }
        visibility.value = when (ktSymbol) {
            is KaNamedClassSymbol -> {
                val exported = ktSymbol.isExported()
                if (exported is SirAvailability.Available) {
                    SirVisibility.PUBLIC
                } else return@withSessions exported
            }
            is KaConstructorSymbol -> {
                if ((ktSymbol.containingSymbol as? KaClassSymbol)?.modality?.isAbstract() != false) {
                    // Hide abstract class constructors from users, but not from other Swift Export modules.
                    SirVisibility.PACKAGE
                } else {
                    SirVisibility.PUBLIC
                }
            }
            is KaNamedFunctionSymbol -> {
                if (!ktSymbol.isExported()) {
                    return@withSessions SirAvailability.Hidden("Function declaration kind isn't supported yet")
                } else {
                    SirVisibility.PUBLIC
                }
            }
            is KaVariableSymbol -> {
                if (ktSymbol.hasHiddenAccessors)
                    return@withSessions SirAvailability.Hidden("Property declaration has hidden accessors")
                else
                    SirVisibility.PUBLIC
            }
            is KaTypeAliasSymbol -> ktSymbol.expandedType.fullyExpandedType.let {
                if (it.isPrimitive || it.isNothingType || it.isFunctionType) {
                    SirVisibility.PUBLIC
                } else when (val availability = it.availability()) {
                    is SirAvailability.Available -> availability.visibility
                    is SirAvailability.Hidden -> return@withSessions SirAvailability.Hidden("Typealias target is hidden")
                    is SirAvailability.Unavailable -> return@withSessions SirAvailability.Unavailable("Typealias target is unavailable")
                }
            }
            else -> return@withSessions SirAvailability.Unavailable("Declaration kind isn't supported yet")
        }

        return@withSessions SirAvailability.Available(visibility.value)
    }

    private fun KaNamedFunctionSymbol.isExported(): Boolean = sirSession.withSessions {
        if (isStatic && !isValueOfOnEnum(this@isExported)) {
            unsupportedDeclarationReporter.report(this@isExported, "static functions are not supported yet.")
            return@withSessions false
        }
        if (origin !in SUPPORTED_SYMBOL_ORIGINS) {
            unsupportedDeclarationReporter.report(this@isExported, "${origin.name.lowercase()} origin is not supported yet.")
            return@withSessions false
        }
        if (isSuspend) {
            unsupportedDeclarationReporter.report(this@isExported, "suspend functions are not supported yet.")
            return@withSessions false
        }
        if (isInline) {
            unsupportedDeclarationReporter.report(this@isExported, "inline functions are not supported yet.")
            return@withSessions false
        }
        if (isClone(this@isExported)) {
            // Cloneable (and its method `clone`) are synthetic on Native, and we don't care about them atm.
            return@withSessions false
        }
        return@withSessions true
    }

    private fun KaNamedClassSymbol.isExported(): SirAvailability = sirSession.withSessions {

        if (hasDeprecatedAncestors()) {
            return@withSessions SirAvailability.Unavailable("Has deprecated ancestors")
        }

        if (!isAllContainingSymbolsExported()) {
            return@withSessions SirAvailability.Hidden("Some containing symbol is hidden")
        }

        if (isFromTemporarilyIgnoredPackage()) {
            return@withSessions SirAvailability.Unavailable("From ignored package")
        }

        // Any is exported as a KotlinBase class.
        if (classId == DefaultTypeClassIds.ANY) {
            return@withSessions SirAvailability.Unavailable("ClassId = Any")
        }
        if (classKind == KaClassKind.ANNOTATION_CLASS || classKind == KaClassKind.ANONYMOUS_OBJECT) {
            return@withSessions SirAvailability.Unavailable("Annotation or Anonymous")
        }
        if (classKind == KaClassKind.ENUM_CLASS) {
            if (superTypes.any { it.symbol?.classId?.asSingleFqName() == FqName("kotlinx.cinterop.CEnum") }) {
                unsupportedDeclarationReporter.report(this@isExported, "C enums are not supported yet.")
                return@withSessions SirAvailability.Unavailable("C enums")
            }
            return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
        }

        if (!(isAllSuperTypesExported(this) { this.isExported() is SirAvailability.Available})) {
            return@withSessions SirAvailability.Hidden("Some super type isn't available")
        }

        if (isInline) {
            unsupportedDeclarationReporter.report(this@isExported, "inline classes are not supported yet.")
            return@withSessions SirAvailability.Unavailable("Inline classes are not supported")
        }

        return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
    }

    private fun KaType.availability(): SirAvailability = sirSession.withSessions {
        (expandedSymbol as? KaDeclarationSymbol)?.sirAvailability(useSiteSession)
            ?: SirAvailability.Unavailable("Type is not a declaration")
    }

    private val KaVariableSymbol.hasHiddenAccessors
        get() = (this as? KaPropertySymbol)?.let {
            it.getter?.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN || it.setter?.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN
        } == true

    private fun KaClassSymbol.hasDeprecatedAncestors(): Boolean = sirSession.withSessions {
        generateSequence(this@hasDeprecatedAncestors) {
            it.superTypes.map { it.symbol }.findIsInstanceAnd<KaClassSymbol> { it.classKind != KaClassKind.INTERFACE }
        }.any {
            it.deprecatedAnnotation?.level.let { it == DeprecationLevel.HIDDEN || it == DeprecationLevel.ERROR }
        }
    }

    private fun KaSession.isValueOfOnEnum(function: KaNamedFunctionSymbol): Boolean {
        with(function) {
            val parent = containingSymbol as? KaClassSymbol ?: return false
            return isStatic && name == StandardNames.ENUM_VALUE_OF && parent.classKind == KaClassKind.ENUM_CLASS
        }
    }

    private fun KaNamedClassSymbol.isAllContainingSymbolsExported(): Boolean = sirSession.withSessions {
        if (containingSymbol !is KaNamedClassSymbol) return@withSessions true
        return@withSessions (containingSymbol as? KaNamedClassSymbol)?.isExported() is SirAvailability.Available
    }
}

private fun KaSession.containsHidesFromObjCAnnotation(symbol: KaAnnotatedSymbol): Boolean {
    return symbol.annotations.any { annotation ->
        val annotationClassId = annotation.classId ?: return@any false
        val annotationClassSymbol = findClass(annotationClassId) ?: return@any false
        ClassId.topLevel(FqName("kotlin.native.HidesFromObjC")) in annotationClassSymbol.annotations
    }
}


private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)
