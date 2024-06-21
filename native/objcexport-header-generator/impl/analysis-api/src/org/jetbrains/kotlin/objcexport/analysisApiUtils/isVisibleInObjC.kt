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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaSymbol.isVisibleInObjC(): Boolean = when (this) {
    is KaCallableSymbol -> this.isVisibleInObjC()
    is KaClassSymbol -> this.isVisibleInObjC()
    else -> false
}

/**
 * Doesn't check visibility of containing symbol, so nested callables are visible
 */
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaCallableSymbol.isVisibleInObjC(): Boolean {
    if (!this.isPublic) return false
    if (this is KaPossibleMultiplatformSymbol && isExpect) return false

    if (this.isHiddenFromObjCByDeprecation()) return false
    if (this.isHiddenFromObjCByAnnotation()) return false
    if (this.isSealedClassConstructor()) return false
    if (this.isComponentNMethod() && !this.directlyOverriddenSymbols.any()) return false
    return true
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KaClassSymbol.isVisibleInObjC(): Boolean {
    // TODO if(specialMapped()) return false
    // TODO if(!defaultType.isObjCObjectType()) return false

    if (!isPublicApi(this)) return false
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

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private val KaCallableSymbol.isPublic: Boolean
    get() {
        /**
         * Visibility check is a temp workaround, since AA doesn't have something similar to K1 [DeclarationDescriptorWithVisibility.isEffectivelyPublicApi]
         * Remove when KT-69122 is implemented
         *
         * See details at [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.shouldBeExposed]
         */
        return (this as KaSymbolWithVisibility).visibility != Visibilities.Internal && isPublicApi(this)
    }

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaSymbol.isSealedClassConstructor(): Boolean {
    if (this !is KaConstructorSymbol) return false
    val containingSymbol = this.containingSymbol ?: return false
    return containingSymbol.modality == KaSymbolModality.SEALED
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@OptIn(ExperimentalContracts::class)
private fun KaSymbol.isComponentNMethod(): Boolean {
    contract {
        returns(true) implies (this@isComponentNMethod is KaNamedFunctionSymbol)
    }

    if (this !is KaNamedFunctionSymbol) return false
    if (!this.isOperator) return false
    val containingClassSymbol = this.containingSymbol as? KaNamedClassSymbol ?: return false
    if (!containingClassSymbol.isData) return false
    return DataClassResolver.isComponentLike(this.name)
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaCallableSymbol.isHiddenFromObjCByAnnotation(): Boolean {
    val overwrittenSymbols = directlyOverriddenSymbols.toList()
    if (overwrittenSymbols.isNotEmpty()) return overwrittenSymbols.first().isHiddenFromObjCByAnnotation()
    return this.containsHidesFromObjCAnnotation()
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaClassSymbol.isHiddenFromObjCByAnnotation(): Boolean {
    val containingSymbol = containingSymbol
    if (containingSymbol is KaClassSymbol && containingSymbol.isHiddenFromObjCByAnnotation()) return true
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
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaAnnotatedSymbol.containsHidesFromObjCAnnotation(): Boolean {
    return annotations.any { annotation ->
        val annotationClassId = annotation.classId ?: return@any false
        val annotationClassSymbol = findClass(annotationClassId) ?: return@any false
        ClassId.topLevel(KonanFqNames.hidesFromObjC) in annotationClassSymbol.annotations
    }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@OptIn(KaExperimentalApi::class)
private fun KaCallableSymbol.isHiddenFromObjCByDeprecation(): Boolean {
    /*
    Note: ObjCExport generally expect overrides of exposed methods to be exposed.
    So don't hide a "deprecated hidden" method which overrides non-hidden one:
     */
    if (deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN &&
        directlyOverriddenSymbols.all { overridden -> overridden.isHiddenFromObjCByDeprecation() }
    ) {
        return true
    }

    val containingClassSymbol = containingSymbol as? KaClassSymbol
    if (containingClassSymbol?.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN) {
        return true
    }

    return false
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@OptIn(KaExperimentalApi::class)
private fun KaClassSymbol.isHiddenFromObjCByDeprecation(): Boolean {
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
    val containingSymbol = containingSymbol
    if (containingSymbol is KaClassSymbol && containingSymbol.isHiddenFromObjCByDeprecation()) {
        return true
    }

    return false
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun KaClassSymbol.isInlined(): Boolean {
    if (this !is KaNamedClassSymbol) return false
    if (this.isInline) return true
    // TODO: There are some native types that are 'implicitly inlined'
    return false
}

private fun KaClassKind.isVisibleInObjC(): Boolean = when (this) {
    CLASS, ENUM_CLASS, INTERFACE, OBJECT, COMPANION_OBJECT -> true
    ANONYMOUS_OBJECT, ANNOTATION_CLASS -> false
}
