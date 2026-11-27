/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.java.util.declarationIdFrom
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.dokka.analysis.kotlin.internal.ClassHierarchy
import org.jetbrains.dokka.analysis.kotlin.internal.FullClassHierarchyBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.Supertypes
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.Location
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.escape
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.TypeTranslator
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.filterOutAny
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.symbols.namedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.analysis.api.types.directSupertypes
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.util.concurrent.ConcurrentHashMap


internal class SymbolFullClassHierarchyBuilder(context: DokkaContext) : FullClassHierarchyBuilder {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val logger = context.logger

    override suspend fun build(module: DModule): ClassHierarchy {
        val map = module.sourceSets.associateWith { ConcurrentHashMap<KdElementId, List<KdElementId>>() }
        module.packages.forEach { visitDocumentable(it, map) }
        return map
    }

    context(_: KaSession)
    private fun collectSupertypesFromKotlinType(
        idWithKType: Pair<KdElementId, KaType>,
        supersMap: MutableMap<KdElementId, Supertypes>
    ) {
        val (declarationId, kotlinType) = idWithKType
        if (supersMap[declarationId] == null) {
            val supertypes = kotlinType.directSupertypes(shouldApproximate = true).filterOutAny().toList()
            val supertypesIdWithKType = supertypes.mapNotNull { supertype ->
                supertype.expandedSymbol?.classId?.let { KdElementId(it.escape()) to supertype }
            }
            supersMap[declarationId] = supertypesIdWithKType.map { it.first }
            supertypesIdWithKType.forEach { collectSupertypesFromKotlinType(it, supersMap) }
        }
    }

    context(_: KaSession)
    private fun collectSupertypesFromPsiClass(
        idWithPsiClass: Pair<KdElementId, PsiClass>,
        supersMap: MutableMap<KdElementId, Supertypes>
    ) {
        val (declarationId, psiClass) = idWithPsiClass
        val supertypes = psiClass.superTypes.mapNotNull { it.resolve() }
            .filterNot { it.qualifiedName == "java.lang.Object" }
        val supertypesIdWithPsiClass = supertypes.mapNotNull { supertype ->
            declarationIdFrom(supertype)?.let { it to supertype }
        }

        if (supersMap[declarationId] == null) {
            supersMap[declarationId] = supertypesIdWithPsiClass.map { it.first }
            supertypesIdWithPsiClass.forEach { collectSupertypesFromPsiClass(it, supersMap) }
        }
    }

    private fun visitDocumentable(
        documentable: Documentable,
        hierarchy: SourceSetDependent<MutableMap<KdElementId, List<KdElementId>>>
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
                            val symbol = psi.namedClassSymbol ?: return@analyze
                            val type = symbol.defaultType
                            val declarationId = symbol.classId?.let { KdElementId(it.escape()) } ?: return@analyze
                            hierarchy[sourceSet]?.let { collectSupertypesFromKotlinType(declarationId to type, it) }
                        }
                    }
                } else if (source is PsiDocumentableSource) {
                    val psi = source.psi as PsiClass
                    analyze(kotlinAnalysis.getModule(sourceSet)) {
                        declarationIdFrom(psi)?.let { id ->
                            hierarchy[sourceSet]?.let { collectSupertypesFromPsiClass(id to psi, it) }
                        }
                    }
                }
            }
        }
    }

    internal class SuperclassesWithKind(
        val typeConstructorWithKind: TypeConstructorWithKind,
        val superclasses: List<TypeConstructorWithKind>
    )

    /**
     * Currently, it works only for Symbols
     */
    internal fun collectKotlinSupertypesWithKind(
        documentable: Iterable<Documentable>,
        sourceSet: DokkaConfiguration.DokkaSourceSet
    ): Map<KdElementId, SuperclassesWithKind> {
        val typeTranslator = TypeTranslator(sourceSet, AnnotationTranslator(logger), logger)
        val hierarchy = mutableMapOf<KdElementId, SuperclassesWithKind>()

        analyze(kotlinAnalysis.getModule(sourceSet)) {
            documentable.filterIsInstance<DClasslike>().forEach {
                val source = it.sources[sourceSet]
                if (source is KtPsiDocumentableSource) {
                    (source.psi as? KtClassOrObject)?.let { psi ->
                        val namedClassSymbol = psi.namedClassSymbol?: return@analyze
                        val location = Location(namedClassSymbol)
                        val type = namedClassSymbol.defaultType
                        collectSupertypesWithKindFromKotlinType(typeTranslator, with(typeTranslator) {
                            toTypeConstructorWithKindFrom(type, location)
                        } to type, hierarchy, location)
                    }
                }  // else if (source is PsiDocumentableSource)  TODO val psi = source.psi as? PsiClass
            }
        }
        return hierarchy
    }

    context(_: KaSession)
    private fun collectSupertypesWithKindFromKotlinType(
        typeTranslator: TypeTranslator,
        typeConstructorWithKindWithKType: Pair<TypeConstructorWithKind, KaType>,
        supersMap: MutableMap<KdElementId, SuperclassesWithKind>,
        location: Location
    ) {
        val (typeConstructorWithKind, kotlinType) = typeConstructorWithKindWithKType

        if (supersMap[typeConstructorWithKind.typeConstructor.declarationId] == null) {
            val supertypes = kotlinType.directSupertypes(shouldApproximate = true).filterOutAny().toList()

            val supertypesIdWithKType = supertypes.map { supertype ->
                with(typeTranslator) {
                    toTypeConstructorWithKindFrom(supertype, location)
                } to supertype
            }
            supersMap[typeConstructorWithKind.typeConstructor.declarationId] =
                SuperclassesWithKind(typeConstructorWithKind, supertypesIdWithKType.map { it.first })
            supertypesIdWithKType.forEach { collectSupertypesWithKindFromKotlinType(
                typeTranslator,
                it,
                supersMap,
                location
            ) }
        }
    }
}
