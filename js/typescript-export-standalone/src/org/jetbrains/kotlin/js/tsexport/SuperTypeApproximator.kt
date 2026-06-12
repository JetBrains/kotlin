/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.directSupertypes
import org.jetbrains.kotlin.analysis.api.components.expandedSymbol
import org.jetbrains.kotlin.analysis.api.components.isAnyType
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.name.StandardClassIds

internal class SuperTypeApproximator {
    private data class ClassWithAppliedArguments(val classSymbol: KaClassLikeSymbol, val appliedArguments: List<KaTypeProjection>)

    private val typeCaches = hashMapOf<ClassWithAppliedArguments, Set<KaType>>()

    context(_: KaSession)
    fun collectSuperTypesTransitiveHierarchyFor(type: KaType): Set<KaType> {
        val klass = (type as? KaClassType)?.symbol ?: return emptySet()
        return typeCaches.computeIfAbsent(ClassWithAppliedArguments(klass, type.typeArguments)) {
            buildSet {
                collectSuperTypesTransitiveHierarchy(type)
            }
        }
    }

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun MutableSet<KaType>.collectSuperTypesTransitiveHierarchy(type: KaType) {
        for (superType in type.directSupertypes(shouldApproximate = true)) {
            collectTransitiveHierarchy(superType)
        }
    }

    // We exclude external interfaces here because they can describe things that don't exist in the TypeScript environment.
    // So adding them into the d.ts file will cause an invalid definition file with TS2552 compilation error.
    private val KaClassSymbol.isExportableExternalClass: Boolean
        get() = classId?.packageFqName?.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE) == true && isExternal && classKind != KaClassKind.INTERFACE

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun MutableSet<KaType>.collectTransitiveHierarchy(type: KaType) {
        if (type !is KaClassType) return
        val klass = type.expandedSymbol ?: return
        val packageFqName = klass.classId?.packageFqName ?: return
        when {
            type.isAnyType || packageFqName.startsWith(StandardClassIds.BASE_JS_PACKAGE) -> return
            klass.isEffectivelyExported(includingImplicitExport = true) || klass.isExportableExternalClass -> add(type)
            else -> collectSuperTypesTransitiveHierarchy(type)
        }
    }
}
