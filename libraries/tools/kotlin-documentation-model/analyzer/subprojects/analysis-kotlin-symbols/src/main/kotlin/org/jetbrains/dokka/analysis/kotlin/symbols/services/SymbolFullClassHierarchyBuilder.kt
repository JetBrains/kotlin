/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromClassLike
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.dokka.analysis.kotlin.internal.ClassHierarchy
import org.jetbrains.dokka.analysis.kotlin.internal.FullClassHierarchyBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.Supertypes
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.util.concurrent.ConcurrentHashMap


internal class SymbolFullClassHierarchyBuilder(val context: DokkaContext) : FullClassHierarchyBuilder {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }

    override suspend fun build(module: DModule): ClassHierarchy {
        val map = module.sourceSets.associateWith { ConcurrentHashMap<DRI, List<DRI>>() }
        module.packages.forEach { visitDocumentable(it, map) }
        return map
    }

    private fun KtAnalysisSession.collectSupertypesFromKtType(
        driWithKType: Pair<DRI, KtType>,
        supersMap: MutableMap<DRI, Supertypes>
    ) {
        val (dri, kotlinType) = driWithKType
        val supertypes = kotlinType.getDirectSuperTypes().filterNot { it.isAny }
        val supertypesDriWithKType = supertypes.mapNotNull { supertype ->
            supertype.expandedClassSymbol?.let {
                getDRIFromClassLike(it) to supertype
            }
        }

        if (supersMap[dri] == null) {
            supersMap[dri] = supertypesDriWithKType.map { it.first }
            supertypesDriWithKType.forEach { collectSupertypesFromKtType(it, supersMap) }
        }
    }

    private fun collectSupertypesFromPsiClass(
        driWithPsiClass: Pair<DRI, PsiClass>,
        supersMap: MutableMap<DRI, Supertypes>
    ) {
        val (dri, psiClass) = driWithPsiClass
        val supertypes = psiClass.superTypes.mapNotNull { it.resolve() }
            .filterNot { it.qualifiedName == "java.lang.Object" }
        val supertypesDriWithPsiClass = supertypes.map { DRI.from(it) to it }

        if (supersMap[dri] == null) {
            supersMap[dri] = supertypesDriWithPsiClass.map { it.first }
            supertypesDriWithPsiClass.forEach { collectSupertypesFromPsiClass(it, supersMap) }
        }
    }

    private fun visitDocumentable(
        documentable: Documentable,
        hierarchy: SourceSetDependent<MutableMap<DRI, List<DRI>>>
    ) {
        if (documentable is WithScope) {
            documentable.classlikes.forEach { visitDocumentable(it, hierarchy) }
        }
        if (documentable is DClasslike) {
            // to build a full class graph,
            // using supertypes from Documentable is not enough since it keeps only one level of hierarchy
            documentable.sources.forEach { (sourceSet, source) ->
                if (source is KtPsiDocumentableSource) {
                    (source.psi as? KtClassOrObject)?.let { psi ->
                        analyze(kotlinAnalysis.getModule(sourceSet)) {
                            val type = psi.getNamedClassOrObjectSymbol()?.buildSelfClassType() ?: return@analyze
                            hierarchy[sourceSet]?.let { collectSupertypesFromKtType(documentable.dri to type, it) }
                        }
                    }
                } else if (source is PsiDocumentableSource) {
                    val psi = source.psi as PsiClass
                    hierarchy[sourceSet]?.let { collectSupertypesFromPsiClass(documentable.dri to psi, it) }
                }
            }
        }
    }

}
