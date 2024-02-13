/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotationInfos
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMultiplatformSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

context(KtAnalysisSession)
internal fun KtSymbol.isVisibleInObjC(): Boolean = when(this) {
    is KtCallableSymbol -> this.isVisibleInObjC()
    is KtClassOrObjectSymbol -> this.isVisibleInObjC()
    else -> false
}

context(KtAnalysisSession)
internal fun KtCallableSymbol.isVisibleInObjC(): Boolean {
    if (this is KtSymbolWithVisibility && !isPublicApi(this)) return false
    if (this is KtPossibleMultiplatformSymbol && isExpect) return false

    if (this.isHiddenFromObjCByDeprecation()) return false
    if (this.isHiddenFromObjCByAnnotation()) return false
    if (this.isSealedClassConstructor()) return false
    if (this.isComponentNMethod() && this.getDirectlyOverriddenSymbols().isEmpty()) return false
    return true
}

context(KtAnalysisSession)
internal fun KtClassOrObjectSymbol.isVisibleInObjC(): Boolean {
    // TODO if(specialMapped()) return false
    // TODO if(!defaultType.isObjCObjectType()) return false

    if (this is KtSymbolWithVisibility && !isPublicApi(this)) return false
    if (this.isHiddenFromObjCByDeprecation()) return false
    if (this.isHiddenFromObjCByAnnotation()) return false
    if (!this.classKind.isVisibleInObjC()) return false
    if (this.isExpect) return false
    if (this.isInlined()) return false
    return true
}

/*
Private utility functions
 */

context(KtAnalysisSession)
private fun KtSymbol.isSealedClassConstructor(): Boolean {
    if (this !is KtConstructorSymbol) return false
    val containingSymbol = this.getContainingSymbol() as? KtSymbolWithModality ?: return false
    return containingSymbol.modality == Modality.SEALED
}

context(KtAnalysisSession)
@OptIn(ExperimentalContracts::class)
private fun KtSymbol.isComponentNMethod(): Boolean {
    contract {
        returns(true) implies (this@isComponentNMethod is KtFunctionSymbol)
    }

    if (this !is KtFunctionSymbol) return false
    if (!this.isOperator) return false
    val containingClassSymbol = this.getContainingSymbol() as? KtNamedClassOrObjectSymbol ?: return false
    if (!containingClassSymbol.isData) return false
    return DataClassResolver.isComponentLike(this.name)
}

context(KtAnalysisSession)
private fun KtCallableSymbol.isHiddenFromObjCByAnnotation(): Boolean {
    val overwrittenSymbols = getDirectlyOverriddenSymbols()
    if (overwrittenSymbols.isNotEmpty()) return overwrittenSymbols.first().isHiddenFromObjCByAnnotation()
    return this.containsHidesFromObjCAnnotation()
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isHiddenFromObjCByAnnotation(): Boolean {
    val containingSymbol = getContainingSymbol()
    if (containingSymbol is KtClassOrObjectSymbol && containingSymbol.isHiddenFromObjCByAnnotation()) return true
    return this.containsHidesFromObjCAnnotation()
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
context(KtAnalysisSession)
private fun KtAnnotatedSymbol.containsHidesFromObjCAnnotation(): Boolean {
    return annotationsList.annotations.any { annotation ->
        val annotationClassId = annotation.classId ?: return@any false
        val annotationClassSymbol = getClassOrObjectSymbolByClassId(annotationClassId) ?: return@any false
        annotationClassSymbol.annotationInfos.any { annotationAnnotation ->
            annotationAnnotation.classId?.asSingleFqName() == KonanFqNames.hidesFromObjC
        }
    }
}

context(KtAnalysisSession)
private fun KtCallableSymbol.isHiddenFromObjCByDeprecation(): Boolean {
    /*
    Note: ObjCExport generally expect overrides of exposed methods to be exposed.
    So don't hide a "deprecated hidden" method which overrides non-hidden one:
     */
    if (deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN &&
        getDirectlyOverriddenSymbols().all { overridden -> overridden.isHiddenFromObjCByDeprecation() }
    ) {
        return true
    }

    val containingClassSymbol = getContainingSymbol() as? KtClassOrObjectSymbol
    if (containingClassSymbol?.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN) {
        return true
    }

    return false
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isHiddenFromObjCByDeprecation(): Boolean {
    if (this.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN) return true

    // Note: ObjCExport requires super class of exposed class to be exposed.
    // So hide a class if its super class is hidden:
    val superClass = getSuperClassSymbolNotAny()
    if (superClass != null && superClass.isHiddenFromObjCByDeprecation()) {
        return true
    }

    // Note: ObjCExport requires enclosing class of exposed class to be exposed.
    // Also in Kotlin hidden class members (including other classes) aren't directly accessible.
    // So hide a class if its enclosing class is hidden:
    val containingSymbol = getContainingSymbol()
    if (containingSymbol is KtClassOrObjectSymbol && containingSymbol.isHiddenFromObjCByDeprecation()) {
        return true
    }

    return false
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isInlined(): Boolean {
    if (this !is KtNamedClassOrObjectSymbol) return false
    if (this.isInline) return true
    // TODO: There are some native types that are 'implicitly inlined'
    return false
}

private fun KtClassKind.isVisibleInObjC(): Boolean = when (this) {
    CLASS, ENUM_CLASS, INTERFACE, OBJECT, COMPANION_OBJECT -> true
    ANONYMOUS_OBJECT, ANNOTATION_CLASS -> false
}
