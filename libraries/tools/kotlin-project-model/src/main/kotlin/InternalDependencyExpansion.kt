/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import java.util.ArrayDeque

interface InternalDependencyExpansion {
    fun expandInternalFragmentDependencies(consumingFragment: KotlinModuleFragment): InternalDependencyExpansionResult
}

class InternalDependencyExpansionResult(
    val entries: Iterable<Entry>
) {
    sealed class ExpansionOutcome(val variantMatchingResults: Iterable<KotlinVariantMatchingResult>) {
        class VisibleFragments(
            val fragments: Iterable<KotlinModuleFragment>,
            variantMatchingResults: Iterable<KotlinVariantMatchingResult>
        ) : ExpansionOutcome(
            variantMatchingResults
        )

        class Failure(variantMatchingResults: Iterable<KotlinVariantMatchingResult>) : ExpansionOutcome(variantMatchingResults)
    }

    class Entry(
        val dependingFragment: KotlinModuleFragment,
        val dependencyFragment: KotlinModuleFragment,
        val outcome: ExpansionOutcome
    )
}

fun InternalDependencyExpansionResult.visibleFragments(): List<KotlinModuleFragment> =
    entries.flatMap { entry -> entry.outcome.visibleFragments() }

fun InternalDependencyExpansionResult.ExpansionOutcome.visibleFragments(): Iterable<KotlinModuleFragment> = when (this) {
    is InternalDependencyExpansionResult.ExpansionOutcome.VisibleFragments -> fragments
    else -> emptyList()
}

class DefaultInternalDependencyExpansion(
    private val variantResolver: ContainingModuleVariantResolver
) : InternalDependencyExpansion {
    interface ContainingModuleVariantResolver {
        fun getChosenVariant(
            dependingVariant: KotlinModuleVariant,
            candidateVariants: Iterable<KotlinModuleVariant>
        ): KotlinVariantMatchingResult
    }

    override fun expandInternalFragmentDependencies(consumingFragment: KotlinModuleFragment): InternalDependencyExpansionResult {
        val visited = mutableSetOf(consumingFragment)
        val answer = mutableListOf<InternalDependencyExpansionResult.Entry>()

        /**
         * For each fragment **t** that we consider a source of declared dependencies that we should expand for [consumingFragment]:
         * - also consider the refines-closure of **t** as sources of declared dependencies
         * - expand the declared dependencies of **t** to a set of fragments **F**
         * - add the expansion result to the answer
         * - also consider all fragments in **F** as sources of declared dependencies
         */
        val declaredDependencySourceQueue = ArrayDeque<KotlinModuleFragment>().apply { add(consumingFragment) }
        while (declaredDependencySourceQueue.isNotEmpty()) {
            val declaredDependencySource = declaredDependencySourceQueue.removeFirst()
            declaredDependencySourceQueue.addAll(declaredDependencySource.refinesClosure.filter(visited::add))
            val declaredDependenciesToExpand = declaredDependencySource.declaredContainingModuleFragmentDependencies
            declaredDependenciesToExpand.forEach { declaredDependency ->
                val answerEntry = expandSingleDeclaredDependency(consumingFragment, declaredDependencySource, declaredDependency)
                answer += answerEntry
                declaredDependencySourceQueue.addAll(answerEntry.outcome.visibleFragments())
            }
        }

        return InternalDependencyExpansionResult(answer)
    }

    private fun expandSingleDeclaredDependency(
        consumingFragment: KotlinModuleFragment,
        declaredDependencySource: KotlinModuleFragment,
        declaredDependency: KotlinModuleFragment
    ): InternalDependencyExpansionResult.Entry {
        /**
         * Go over the variants containing the fragment that we infer the expansion for (note: it's the original consuming fragment, which
         * may not necessarily be the same as the fragment holding the declared dependency if the latter comes from the closure).
         *
         * For each such containing variant v_i, let w_i be the variant containing the declared dependency fragment. Failure to choose
         * prevents us from inferring the dependency expansion and is reported as a failure to expand the dependency.
         *
         * Then intersect the refined fragments of each w_i and use the intersection as the result.
         */

        val (consumingVariants, producingVariants) =
            listOf(consumingFragment, declaredDependency).map { it.containingModule.variantsContainingFragment(it).toSet() }

        val chosenVariants = consumingVariants.map { consumingVariant ->
            if (consumingVariant in producingVariants)
                VariantMatch(consumingVariant, consumingVariant.containingModule, consumingVariant)
            else
                variantResolver.getChosenVariant(consumingVariant, producingVariants)
        }

        val mismatchedConsumingVariants = chosenVariants.filter { it !is VariantMatch }

        val outcome =
            if (mismatchedConsumingVariants.isNotEmpty())
                InternalDependencyExpansionResult.ExpansionOutcome.Failure(chosenVariants)
            else
                InternalDependencyExpansionResult.ExpansionOutcome.VisibleFragments(
                    chosenVariants.map { (it as VariantMatch).chosenVariant.refinesClosure }.reduce { acc, it -> acc.intersect(it) },
                    chosenVariants
                )

        return InternalDependencyExpansionResult.Entry(declaredDependencySource, declaredDependency, outcome)
    }
}