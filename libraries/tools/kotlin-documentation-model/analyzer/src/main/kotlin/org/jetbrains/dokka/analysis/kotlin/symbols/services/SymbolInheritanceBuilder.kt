/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.analysis.kotlin.internal.InheritanceBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.InheritanceNode
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

/**
 * This is copy-pasted from org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.DescriptorInheritanceBuilder and adapted for symbols
 */
internal class SymbolInheritanceBuilder(context: DokkaContext) : InheritanceBuilder {
    private val symbolFullClassHierarchyBuilder =
        context.plugin<InternalKotlinAnalysisPlugin>().querySingle { fullClassHierarchyBuilder }

    override fun build(documentables: Map<DRI, Documentable>): List<InheritanceNode> {

        // this statement is copy-pasted from the version for Descriptors
        val psiInheritanceTree =
            documentables.flatMap { (_, v) -> (v as? WithSources)?.sources?.values.orEmpty() }
                .filterIsInstance<PsiDocumentableSource>().mapNotNull { it.psi as? PsiClass }
                .flatMap(::gatherPsiClasses)
                .flatMap { entry -> entry.second.map { it to entry.first } }
                .let {
                    it + it.map { it.second to null }
                }
                .groupBy({ it.first }) { it.second }
                .map { it.key to it.value.filterNotNull().distinct() }
                .map { (k, v) ->
                    InheritanceNode(
                        DRI.from(k),
                        v.map { InheritanceNode(DRI.from(it)) },
                        k.supers.filter { it.isInterface }.map { DRI.from(it) },
                        k.isInterface
                    )

                }

        // copy-pasted from stdlib 1.5
        fun <T, R : Any> Iterable<T>.firstNotNullOfOrNull(transform: (T) -> R?): R? {
            for (element in this) {
                val result = transform(element)
                if (result != null) {
                    return result
                }
            }
            return null
        }

        val jvmSourceSet =
            documentables.values.firstNotNullOfOrNull { it.sourceSets.find { it.analysisPlatform == Platform.jvm } }
        if (jvmSourceSet == null)
            return psiInheritanceTree

        val typeConstructorsMap =
            (symbolFullClassHierarchyBuilder as? SymbolFullClassHierarchyBuilder)?.collectKotlinSupertypesWithKind(
                documentables.values,
                jvmSourceSet
            )
                ?: throw IllegalStateException("Unexpected symbolFullClassHierarchyBuildertype") // TODO: https://github.com/Kotlin/dokka/issues/3225 Unify FullClassHierarchyBuilder and InheritanceBuilder into one builder

        fun ClassKind.isInterface() = this == KotlinClassKindTypes.INTERFACE || this == JavaClassKindTypes.INTERFACE
        val symbolsInheritanceTree = typeConstructorsMap.map { (dri, superclasses) ->
            InheritanceNode(
                dri,
                superclasses.superclasses.map { InheritanceNode(it.typeConstructor.dri) },
                superclasses.superclasses.filter { it.kind.isInterface() }.map { it.typeConstructor.dri },
                isInterface = superclasses.typeConstructorWithKind.kind.isInterface()
            )
        }

        return psiInheritanceTree + symbolsInheritanceTree
    }

    private fun gatherPsiClasses(psi: PsiClass): List<Pair<PsiClass, List<PsiClass>>> = psi.supers.toList().let { l ->
        listOf(psi to l) + l.flatMap { gatherPsiClasses(it) }
    }
}
