/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromClassLike
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.dokka.analysis.kotlin.internal.ClassHierarchy
import org.jetbrains.dokka.analysis.kotlin.internal.FullClassHierarchyBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.Supertypes
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.Location
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.TypeTranslator
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.util.concurrent.ConcurrentHashMap


internal class SymbolFullClassHierarchyBuilder(context: DokkaContext) : FullClassHierarchyBuilder {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val logger = context.logger

    override suspend fun build(module: DModule): ClassHierarchy {
        val map = module.sourceSets.associateWith { ConcurrentHashMap<DRI, List<DRI>>() }
        module.packages.forEach { visitDocumentable(it, map) }
        return map
    }

    private fun KaSession.collectSupertypesFromKotlinType(
        driWithKType: Pair<DRI, KaType>,
        supersMap: MutableMap<DRI, Supertypes>
    ) {
        val (dri, kotlinType) = driWithKType
        if (supersMap[dri] == null) {
            val supertypes = kotlinType.directSupertypes(shouldApproximate = true).filterNot { it.isAnyType }.toList()
            val supertypesDriWithKType = supertypes.mapNotNull { supertype ->
                supertype.expandedSymbol?.let {
                    getDRIFromClassLike(it) to supertype
                }
            }
            supersMap[dri] = supertypesDriWithKType.map { it.first }
            supertypesDriWithKType.forEach { collectSupertypesFromKotlinType(it, supersMap) }
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
                            val type = psi.namedClassSymbol?.defaultType ?: return@analyze
                            hierarchy[sourceSet]?.let { collectSupertypesFromKotlinType(documentable.dri to type, it) }
                        }
                    }
                } else if (source is PsiDocumentableSource) {
                    val psi = source.psi as PsiClass
                    hierarchy[sourceSet]?.let { collectSupertypesFromPsiClass(documentable.dri to psi, it) }
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
    ): Map<DRI, SuperclassesWithKind> {
        val typeTranslator = TypeTranslator(sourceSet, AnnotationTranslator(logger), logger)
        val hierarchy = mutableMapOf<DRI, SuperclassesWithKind>()

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

    private fun KaSession.collectSupertypesWithKindFromKotlinType(
        typeTranslator: TypeTranslator,
        typeConstructorWithKindWithKType: Pair<TypeConstructorWithKind, KaType>,
        supersMap: MutableMap<DRI, SuperclassesWithKind>,
        location: Location
    ) {
        val (typeConstructorWithKind, kotlinType) = typeConstructorWithKindWithKType

        if (supersMap[typeConstructorWithKind.typeConstructor.dri] == null) {
            val supertypes = kotlinType.directSupertypes(shouldApproximate = true).filterNot { it.isAnyType }.toList()

            val supertypesDriWithKType = supertypes.map { supertype ->
                with(typeTranslator) {
                    toTypeConstructorWithKindFrom(supertype, location)
                } to supertype
            }
            supersMap[typeConstructorWithKind.typeConstructor.dri] =
                SuperclassesWithKind(typeConstructorWithKind, supertypesDriWithKType.map { it.first })
            supertypesDriWithKType.forEach { collectSupertypesWithKindFromKotlinType(
                typeTranslator,
                it,
                supersMap,
                location
            ) }
        }
    }
}
