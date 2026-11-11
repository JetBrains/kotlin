/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirAvailability
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.kotlin.sir.providers.utils.hasNonPublicOptIns
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.providers.utils.isFromTemporarilyIgnoredPackage
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.findIsInstanceAnd

public class SirVisibilityCheckerImpl(
    private val sirSession: SirSession,
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
    private val enableCoroutinesSupport: Boolean,
    private val hiddenModules: List<KaModule>
) : SirVisibilityChecker {
    override fun KaDeclarationSymbol.sirAvailability(): SirAvailability = sirSession.withSessions {
        val ktSymbol = this@sirAvailability

        if (ktSymbol is KaClassSymbol && ktSymbol.classId?.let { sirSession.isClassIdSupported(it) } == true) {
            return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
        }

        val visibility = object {
            var value: SirVisibility = SirVisibility.entries.last()
                set(newValue) {
                    field = minOf(field, newValue)
                }
        }

        if (hiddenModules.contains(ktSymbol.containingModule)) {
            return@withSessions SirAvailability.Hidden("Declaration comes from a module excluded from Swift Export")
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
        if (!ktSymbol.visibility.isExposedToSwift) {
            visibility.value = SirVisibility.PRIVATE
        }
        // Hidden declarations are, well, hidden.
        val deprecatedAnnotation = ktSymbol.deprecatedAnnotation
        if (deprecatedAnnotation?.level == DeprecationLevel.HIDDEN) {
            visibility.value = SirVisibility.PRIVATE
        }
        if (deprecatedAnnotation?.level == DeprecationLevel.ERROR && (ktSymbol.containingDeclaration as? KaNamedClassSymbol)?.classKind == KaClassKind.INTERFACE) {
            return@withSessions SirAvailability.Unavailable("Protocol members with DeprecationLevel.ERROR are unsupported")
        }
        if (containsHidesFromObjCAnnotation(ktSymbol)) {
            return@withSessions SirAvailability.Unavailable("Declaration is @HiddenFromObjC")
        }
        if (ktSymbol is KaNamedFunctionSymbol && ktSymbol.overridesKotlinAnyMember()) {
            // `toString`, `hashCode` and `equals` pre provided as `description`, `hash`, and `isEqual` by KotlinBase.
            return@withSessions SirAvailability.Unavailable("kotlin.Any members are exposed as through KotlinBase")
        }
        when (val parentAvailability = (ktSymbol.containingSymbol as? KaDeclarationSymbol?)?.sirAvailability()) {
            is SirAvailability.Unavailable -> return@withSessions SirAvailability.Unavailable("Declaration's lexical parent is unavailable")
            is SirAvailability.Available -> visibility.value = parentAvailability.visibility
            is SirAvailability.Hidden, null -> {}
        }
        if (ktSymbol.hasNonPublicOptIns) {
            return@withSessions SirAvailability.Unavailable("Declarations with non-public OptIn requirements are unsupported")
        }
        visibility.value = when (ktSymbol) {
            is KaNamedClassSymbol -> {
                val exported = ktSymbol.isExported()
                if (exported is SirAvailability.Available) {
                    exported.visibility
                } else return@withSessions exported
            }
            is KaConstructorSymbol -> {
                SirVisibility.PUBLIC
            }
            is KaNamedFunctionSymbol -> {
                if (!ktSymbol.isExported()) {
                    return@withSessions SirAvailability.Hidden("Function declaration kind isn't supported yet")
                } else {
                    SirVisibility.PUBLIC
                }
            }
            is KaVariableSymbol -> {
                val exported = ktSymbol.isExported()
                if (exported is SirAvailability.Available) {
                    exported.visibility
                } else return@withSessions exported
            }
            is KaPropertySetterSymbol -> SirVisibility.PUBLIC
            is KaTypeAliasSymbol -> ktSymbol.expandedType.fullyExpandedType.let { type ->
                if (type is KaFunctionType) {
                    val types = buildList {
                        addAll(type.contextParameterTypes)
                        addIfNotNull(type.receiverType)
                        addAll(type.parameterTypes)
                        add(type.returnType)
                    }
                    var visibility = SirVisibility.PUBLIC
                    for (type in types) {
                        when (val availability = type.availability()) {
                            is SirAvailability.Available -> visibility = minOf(visibility, availability.visibility)
                            is SirAvailability.Hidden -> return@withSessions SirAvailability.Hidden("Type in functional typealias is hidden")
                            is SirAvailability.Unavailable -> return@withSessions SirAvailability.Unavailable("Type in functional typealias is unavailable")
                        }
                    }
                    visibility
                } else if (type.classId in KaStandardTypeClassIds.PRIMITIVES || type.classId == KaStandardTypeClassIds.NOTHING) {
                    SirVisibility.PUBLIC
                } else when (val availability = type.availability()) {
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
        // TODO(KT-87720): Support companion blocks and extensions.
        @OptIn(KaExperimentalApi::class)
        if (isCompanion && !isValueOfOnEnum(this@isExported)) {
            unsupportedDeclarationReporter.report(this@isExported, "companion blocks and extensions are not supported yet.")
            return@withSessions false
        }
        if (origin !in SUPPORTED_SYMBOL_ORIGINS) {
            unsupportedDeclarationReporter.report(this@isExported, "${origin.name.lowercase()} origin is not supported yet.")
            return@withSessions false
        }
        if (isSuspend && !enableCoroutinesSupport) {
            unsupportedDeclarationReporter.report(this@isExported, "suspend functions are not supported yet.")
            return@withSessions false
        }
        if (typeParameters.any { it.isReified }) {
            unsupportedDeclarationReporter.report(this@isExported, "inline functions with reified type parameters are not supported yet.")
            return@withSessions false
        }
        return@withSessions true
    }

    private fun KaNamedFunctionSymbol.overridesKotlinAnyMember(): Boolean = sirSession.withSessions {
        allOverriddenSymbols.any { (it.containingDeclaration as? KaClassSymbol)?.classId == KaStandardTypeClassIds.ANY }
    }

    private val activeExportChecks = ThreadLocal.withInitial { HashSet<KaNamedClassSymbol>() }

    private fun KaNamedClassSymbol.isExported(): SirAvailability = sirSession.withSessions {
        val active = activeExportChecks.get()
        if (!active.add(this@isExported)) {
            return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
        }
        try {
            if (hasHiddenAncestors()) {
                return@withSessions SirAvailability.Unavailable("Has hidden ancestors")
            }

            if (!isAllContainingSymbolsExported()) {
                return@withSessions SirAvailability.Hidden("Some containing symbol is hidden")
            }

            if (isFromTemporarilyIgnoredPackage()) {
                return@withSessions SirAvailability.Unavailable("From ignored package")
            }

            if (isInner && generateSequence(containingSymbol as? KaNamedClassSymbol) { it.containingSymbol as? KaNamedClassSymbol }.any { it.typeParameters.isNotEmpty() }) {
                unsupportedDeclarationReporter.report(this@isExported, "inner classes inside generic classes are not supported yet.")
                return@withSessions SirAvailability.Unavailable("Inner classes inside generic classes are not supported yet")
            }

            // Any is exported as a KotlinBase class.
            if (classId == KaStandardTypeClassIds.ANY) {
                return@withSessions SirAvailability.Unavailable("ClassId = Any")
            }
            if (classKind == KaClassKind.ANNOTATION_CLASS || classKind == KaClassKind.ANONYMOUS_OBJECT) {
                return@withSessions SirAvailability.Unavailable("Annotation or Anonymous")
            }
            if (isKotlinObjCClass()) {
                unsupportedDeclarationReporter.report(this@isExported, "Kotlin subclasses of Objective-C classes are not supported.")
                return@withSessions SirAvailability.Unavailable("Kotlin subclass of an Objective-C class")
            }
            if (classKind == KaClassKind.ENUM_CLASS) {
                if (superTypes.any { it.symbol?.classId?.asSingleFqName() == FqName("kotlinx.cinterop.CEnum") }) {
                    unsupportedDeclarationReporter.report(this@isExported, "C enums are not supported yet.")
                    return@withSessions SirAvailability.Unavailable("C enums")
                }
                return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
            }

            val visitedSupers = HashSet<KaNamedClassSymbol>()
            visitedSupers.add(this@isExported)
            fun areAllSuperTypesExported(sym: KaNamedClassSymbol): Boolean {
                for (sup in sym.superTypes) {
                    val supSym = sup.expandedSymbol as? KaNamedClassSymbol ?: return false
                    if (supSym.classId == KaStandardTypeClassIds.ANY) continue
                    if (!visitedSupers.add(supSym)) continue
                    if (supSym.isExported() !is SirAvailability.Available) return false
                    if (!areAllSuperTypesExported(supSym)) return false
                }
                return true
            }
            if (!areAllSuperTypesExported(this@isExported)) {
                return@withSessions SirAvailability.Hidden("Some super type isn't available")
            }

            return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
        } finally {
            active.remove(this@isExported)
        }
    }

    private fun KaVariableSymbol.isExported(): SirAvailability = sirSession.withSessions {
        if (hasHiddenGetter) {
            return@withSessions SirAvailability.Hidden("Property declaration has hidden accessors")
        }
        if (typeParameters.any { it.isReified }) {
            unsupportedDeclarationReporter.report(this@isExported, "properties with reified type parameters are not supported yet.")
            return@withSessions SirAvailability.Hidden("Properties with reified type parameters are not supported yet.")
        }
        // TODO(KT-87720): Support companion blocks and extensions.
        @OptIn(KaExperimentalApi::class)
        if (isCompanion && this !is KaEnumEntrySymbol) {
            return@withSessions SirAvailability.Hidden("companion blocks and extensions are not supported yet.")
        }
        return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
    }

    private fun KaType.availability(): SirAvailability = sirSession.withSessions {
        if (this@availability is KaTypeParameterType) {
            return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
        }
        if (classId in KaStandardTypeClassIds.PRIMITIVES ||
            classId == KaStandardTypeClassIds.NOTHING ||
            classId == KaStandardTypeClassIds.UNIT ||
            classId == KaStandardTypeClassIds.ANY
        ) {
            return@withSessions SirAvailability.Available(SirVisibility.PUBLIC)
        }
        (expandedSymbol as? KaDeclarationSymbol)?.sirAvailability()
            ?: SirAvailability.Unavailable("Type is not a declaration")
    }

    private val KaVariableSymbol.hasHiddenGetter
        get() = (this as? KaPropertySymbol)?.getter?.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN

    private fun KaClassSymbol.hasHiddenAncestors(): Boolean = sirSession.withSessions {
        generateSequence(this@hasHiddenAncestors) { symbol ->
            symbol.superTypes.map { it.symbol }.findIsInstanceAnd<KaClassSymbol> { it.classKind != KaClassKind.INTERFACE }
        }.drop(1).any { symbol ->
            symbol.deprecatedAnnotation?.level.let { it == DeprecationLevel.HIDDEN }
        }
    }

    context(ka: KaSession)
    private fun isValueOfOnEnum(function: KaNamedFunctionSymbol): Boolean {
        with(function) {
            val parent = containingSymbol as? KaClassSymbol ?: return false
            return isStatic && name == StandardNames.ENUM_VALUE_OF && parent.classKind == KaClassKind.ENUM_CLASS
        }
    }

    private fun KaNamedClassSymbol.isKotlinObjCClass(): Boolean = sirSession.withSessions {
        val externalObjCClassClassId = ClassId.fromString("kotlinx/cinterop/ExternalObjCClass")
        val objCObjectClassId = ClassId.fromString("kotlinx/cinterop/ObjCObject")

        if (origin == KaSymbolOrigin.NATIVE_FORWARD_DECLARATION) return@withSessions false
        if (classId?.packageFqName == objCObjectClassId.packageFqName) return@withSessions false

        val isImportedFromObjC = generateSequence<KaClassSymbol>(this@isKotlinObjCClass) { it.containingSymbol as? KaClassSymbol }
            .any { externalObjCClassClassId in it.annotations }
        if (isImportedFromObjC) return@withSessions false

        val visited = HashSet<KaClassSymbol>()
        fun hasObjCSupertype(sym: KaClassSymbol): Boolean {
            if (!visited.add(sym)) return false
            for (sup in sym.superTypes) {
                val supSym = sup.expandedSymbol ?: continue
                if (supSym.classId == objCObjectClassId || hasObjCSupertype(supSym)) return true
            }
            return false
        }
        hasObjCSupertype(this@isKotlinObjCClass)
    }

    private fun KaNamedClassSymbol.isAllContainingSymbolsExported(): Boolean = sirSession.withSessions {
        if (containingSymbol !is KaNamedClassSymbol) return@withSessions true
        return@withSessions (containingSymbol as? KaNamedClassSymbol)?.isExported() is SirAvailability.Available
    }
}

private val KaSymbolVisibility.isExposedToSwift: Boolean
    get() = when (this) {
        KaSymbolVisibility.PUBLIC, KaSymbolVisibility.PACKAGE_PROTECTED -> true
        else -> false
    }

context(ka: KaSession)
private fun containsHidesFromObjCAnnotation(symbol: KaAnnotatedSymbol): Boolean {
    return symbol.annotations.any { annotation ->
        val annotationClassId = annotation.classId ?: return@any false
        val annotationClassSymbol = findClass(annotationClassId) ?: return@any false
        ClassId.topLevel(FqName("kotlin.native.HidesFromObjC")) in annotationClassSymbol.annotations
    }
}


private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)

