/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


internal fun KaSession.isVisibleInObjC(symbol: KaSymbol?): Boolean = when (symbol) {
    is KaCallableSymbol -> isVisibleInObjC(symbol)
    is KaClassSymbol -> isVisibleInObjC(symbol)
    else -> false
}

/**
 * Doesn't check visibility of containing symbol, so nested callables are visible
 */
internal fun KaSession.isVisibleInObjC(symbol: KaCallableSymbol): Boolean {
    if (!isPublic(symbol)) return false
    if (symbol.isExpect) return false

    if (isHiddenFromObjCByDeprecation(symbol)) return false
    if (isHiddenFromObjCByAnnotation(symbol)) return false
    if (isSealedClassConstructor(symbol)) return false
    if (isComponentNMethod(symbol) && !symbol.directlyOverriddenSymbols.any()) return false
    return true
}

internal fun KaSession.isVisibleInObjC(symbol: KaClassSymbol): Boolean {
    // TODO if(specialMapped()) return false
    // TODO if(!defaultType.isObjCObjectType()) return false

    if (!isPublicApi(symbol)) return false
    if (isHiddenFromObjCByDeprecation(symbol)) return false
    if (isHiddenFromObjCByAnnotation(symbol)) return false
    if (!symbol.classKind.isVisibleInObjC()) return false
    if (symbol.isExpect) return false
    if (isInlined(symbol)) return false
    return true
}

/*
Private utility functions
 */

private fun KaSession.isPublic(symbol: KaCallableSymbol): Boolean {
    /**
     * Visibility check is a temp workaround, since AA doesn't have something similar to K1 [DeclarationDescriptorWithVisibility.isEffectivelyPublicApi]
     * Remove when KT-69122 is implemented
     *
     * See details at [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.shouldBeExposed]
     */
    return symbol.visibility != KaSymbolVisibility.INTERNAL && isPublicApi(symbol)
}

private fun KaSession.isSealedClassConstructor(symbol: KaSymbol): Boolean {
    if (symbol !is KaConstructorSymbol) return false
    val containingSymbol = symbol.containingDeclaration ?: return false
    return containingSymbol.modality == KaSymbolModality.SEALED
}

@OptIn(ExperimentalContracts::class)
private fun KaSession.isComponentNMethod(symbol: KaSymbol): Boolean {
    contract {
        returns(true) implies (this@isComponentNMethod is KaNamedFunctionSymbol)
    }

    if (symbol !is KaNamedFunctionSymbol) return false
    if (!symbol.isOperator) return false
    val containingClassSymbol = symbol.containingDeclaration as? KaNamedClassSymbol ?: return false
    if (!containingClassSymbol.isData) return false
    return DataClassResolver.isComponentLike(symbol.name)
}

private fun KaSession.isHiddenFromObjCByAnnotation(callable: KaCallableSymbol): Boolean {
    val overwrittenSymbols = callable.directlyOverriddenSymbols.toList()
    if (overwrittenSymbols.isNotEmpty()) return isHiddenFromObjCByAnnotation(overwrittenSymbols.first())
    return containsHidesFromObjCAnnotation(callable)
}

private fun KaSession.isHiddenFromObjCByAnnotation(symbol: KaClassSymbol): Boolean {
    val containingSymbol = symbol.containingDeclaration
    if (containingSymbol is KaClassSymbol && isHiddenFromObjCByAnnotation(containingSymbol)) return true
    return containsHidesFromObjCAnnotation(symbol)
}

/**
 * Returns if [this] symbol is annotated with some annotation that effectively hides the symbol from ObjC.
 *
 * **Example: Using pre-defined 'HiddenFromObjC' annotation**
 * ```kotlin
 * @HiddenFromObjC
 * fun foo() = Unit
 * ```
 *
 * **Example: Using a custom 'internal api marker'**
 * ```kotlin
 * @HidesFromObjC
 * annotation class MyInternalApi
 *
 * @MyInternalApi
 * fun foo()
 * ```
 *
 */
private fun KaSession.containsHidesFromObjCAnnotation(symbol: KaAnnotatedSymbol): Boolean {
    return symbol.annotations.any { annotation ->
        val annotationClassId = annotation.classId ?: return@any false
        val annotationClassSymbol = findClass(annotationClassId) ?: return@any false
        ClassId.topLevel(KonanFqNames.hidesFromObjC) in annotationClassSymbol.annotations
    }
}

@OptIn(KaExperimentalApi::class)
private fun KaSession.isHiddenFromObjCByDeprecation(callable: KaCallableSymbol): Boolean {
    /*
    Note: ObjCExport generally expect overrides of exposed methods to be exposed.
    So don't hide a "deprecated hidden" method which overrides non-hidden one:
     */
    if (callable.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN &&
        callable.directlyOverriddenSymbols.all { overridden -> isHiddenFromObjCByDeprecation(overridden) }
    ) {
        return true
    }

    val containingClassSymbol = callable.containingDeclaration as? KaClassSymbol
    if (containingClassSymbol?.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN) {
        return true
    }

    return false
}

@OptIn(KaExperimentalApi::class)
private fun KaSession.isHiddenFromObjCByDeprecation(symbol: KaClassSymbol): Boolean {
    if (symbol.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN) return true

    // Note: ObjCExport requires super class of exposed class to be exposed.
    // So hide a class if its super class is hidden:
    val superClass = getSuperClassSymbolNotAny(symbol)
    if (superClass != null && isHiddenFromObjCByDeprecation(superClass)) {
        return true
    }

    // Note: ObjCExport requires enclosing class of exposed class to be exposed.
    // Also in Kotlin hidden class members (including other classes) aren't directly accessible.
    // So hide a class if its enclosing class is hidden:
    val containingSymbol = symbol.containingDeclaration
    if (containingSymbol is KaClassSymbol && isHiddenFromObjCByDeprecation(containingSymbol)) {
        return true
    }

    return false
}

private fun KaSession.isInlined(symbol: KaClassSymbol): Boolean {
    if (symbol !is KaNamedClassSymbol) return false
    if (symbol.isInline) return true
    // TODO: There are some native types that are 'implicitly inlined'
    return false
}

private fun KaClassKind.isVisibleInObjC(): Boolean = when (this) {
    CLASS, ENUM_CLASS, INTERFACE, OBJECT, COMPANION_OBJECT -> true
    ANONYMOUS_OBJECT, ANNOTATION_CLASS -> false
}
