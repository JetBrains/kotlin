/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics

import java.io.Serializable

internal object UklibFragmentsChecker {
    data class FragmentToCheck(
        val identifier: String,
        val attributes: Set<String>,
    ) : Serializable {
        // Here refines means refines within the same Uklib module (i.e. source set dependsOn)
        fun refines(fragment: FragmentToCheck): Boolean = attributes.isSubsetOf(fragment.attributes)
        private fun <E> Set<E>.isSubsetOf(another: Set<E>): Boolean = another.containsAll(this)
        override fun hashCode(): Int = identifier.hashCode()
        override fun equals(other: Any?): Boolean = other is FragmentToCheck && other.identifier == identifier
    }

    sealed class Violation : Serializable {
        /**
         * Sanity and input graph checks
         */
        object EmptyRefinementGraph : Violation() {
            private fun readResolve(): Any = EmptyRefinementGraph
        }
        data class MissingFragment(val identifier: String) : Violation()
        data class FragmentWithEmptyAttributes(val fragment: FragmentToCheck) : Violation()
        data class FirstEncounteredCycle(val cycle: List<FragmentToCheck>) : Violation()

        /**
         * Multiple-same targets and bamboos
         */
        data class DuplicateAttributesFragments(
            val attributes: Set<String>,
            val duplicates: Set<FragmentToCheck>,
        ) : Violation()

        /**
         * Checks for:
         * - "Targets of `F1` are compatible with `F2` => Fragment `F1` refines fragment `F2`"
         * - "Fragment `F1` refines fragment `F2` => Targets of `F1` are compatible with `F2`"
         * - "Fragment `F` doesn't have refiners <=> it has exactly one `KotlinTarget`"
         */
        data class UnderRefinementViolation(
            val fragment: FragmentToCheck,
            val underRefinedFragments: Set<FragmentToCheck>,
            val actuallyRefinedFragments: Set<FragmentToCheck>,
        ) : Violation()

        data class IncompatibleRefinementViolation(
            val fragment: FragmentToCheck,
            val incompatibleFragments: Set<FragmentToCheck>,
        ) : Violation()

        data class OrphanedIntermediateFragment(val fragment: FragmentToCheck) : Violation()
    }

    /**
     * Provide the fragment graph in the form
     * - FragmentToCheck("iosMain") refines setOf("commonMain"),
     * - FragmentToCheck("commonMain") refines emptySet()
     */
    fun findViolationsInSourceSetGraph(
        refinementEdges: Map<FragmentToCheck, Set<String>>,
    ): Set<Violation> {
        val violations = hashSetOf<Violation>()
        if (refinementEdges.isEmpty()) {
            violations.add(Violation.EmptyRefinementGraph)
            return violations
        }

        /**
         * Check that the passed graph has all vertices and the attributes for each vertex are not empty
         */
        val fragments = refinementEdges.keys
        val fragmentByIdentifier = fragments.associateBy { it.identifier }
        val missingFragments = checkAllRefineesHaveCorresponsingFragment(refinementEdges, fragmentByIdentifier)
        if (missingFragments.isNotEmpty()) {
            violations.addAll(missingFragments)
            // Checks below rely on being able to access all vertices in the graph
            return violations
        }

        // Attributes must not be empty
        val fragmentsWithEmptyAttributes = refinementEdges.keys.filter {
            it.attributes.isEmpty()
        }.toHashSet()
        if (fragmentsWithEmptyAttributes.isNotEmpty()) {
            violations.addAll(
                fragmentsWithEmptyAttributes.map {
                    Violation.FragmentWithEmptyAttributes(it)
                }
            )
            // Checks below rely on every fragment to not be missing attributes
            return violations
        }

        /**
         * Detect cycles to make sure we are working with a DAG
         *
         * Find and return the first encountered cycle similar to what we do in CircularDependsOnEdges
         */
        val cycle = findFirstCycle(fragments, refinementEdges, fragmentByIdentifier)
        if (cycle != null) {
            violations.add(cycle)
            return violations
        }

        /**
         * Violations of "It's forbidden to have several fragments with the same attributes in uklib"
         *
         * i.e. detect multiple-same targets and bamboos
         */
        val duplicateAttributesFragments = findFragmentsWithDuplicateAttributes(fragments)
        duplicateAttributesFragments.forEach {
            violations.add(
                Violation.DuplicateAttributesFragments(
                    it.key,
                    it.value
                )
            )
        }

        /**
         * Violations of "Fragment `F1` refines fragment `F2` <=> targets of `F1` are compatible with `F2`"
         *
         * 1. For each fragment using the attributes find the transitive closure of other fragments it must refine and the fragments it
         * actually refines
         */
        val expectedTransitiveClosureOfRefinedFragments = expectedTransitiveRefinementEdges(fragments)
        val actualTransitiveClosureOfRefinedFragments = actualTransitiveRefinementEdges(fragments, refinementEdges, fragmentByIdentifier)
        val allDuplicateAttributesFragments = duplicateAttributesFragments.values.flatten()

        fragments.forEach { fragment ->
            val actuallyRefinedFragments = actualTransitiveClosureOfRefinedFragments[fragment]!!
            val expectedRefinementFragments = expectedTransitiveClosureOfRefinedFragments[fragment]!!

            /**
             * "Targets of `F1` are compatible with `F2` => Fragment `F1` refines fragment `F2`"
             *
             * 2. Check that the fragment actually refined all the fragments it was supposed to
             */
            val underRefinedFragments = expectedRefinementFragments
                .subtract(actuallyRefinedFragments)
                /**
                 * Allow bamboos in [expectedRefinementFragments], but then don't report them as under or incompatible refinements
                 */
                .subtract(allDuplicateAttributesFragments)
            if (underRefinedFragments.isNotEmpty()) {
                violations.add(
                    Violation.UnderRefinementViolation(
                        fragment = fragment,
                        underRefinedFragments = underRefinedFragments,
                        actuallyRefinedFragments = actuallyRefinedFragments,
                    )
                )
            }

            /**
             * "Fragment `F1` refines fragment `F2` => Targets of `F1` are compatible with `F2`"
             *
             * 3. Check that the fragment didn't refine any fragments it wasn't compatible with
             */
            val incompatibleFragments = actuallyRefinedFragments
                .subtract(expectedRefinementFragments)
            if (incompatibleFragments.isNotEmpty()) {
                violations.add(
                    Violation.IncompatibleRefinementViolation(
                        fragment = fragment,
                        incompatibleFragments = incompatibleFragments,
                    )
                )
            }
        }

        /**
         * Check for "Fragment `F` doesn't have refiners <=> it has exactly one `KotlinTarget`"
         *
         * Left to right is explicitly checked here. Right to left is checked by multiple-same targets and incompatible refinement violations
         */
        val orphanedIntermediateFragments = fragments.filter { it.attributes.size > 1 }.toHashSet().subtract(
            refinementEdges.values.flatten().map {
                fragmentByIdentifier[it]!!
            }.toSet()
        )
        violations.addAll(
            orphanedIntermediateFragments.map {
                Violation.OrphanedIntermediateFragment(it)
            }
        )

        return violations
    }

    private fun findFragmentsWithDuplicateAttributes(fragments: Set<FragmentToCheck>): Map<Set<String>, MutableSet<FragmentToCheck>> {
        val attributeSets: MutableMap<Set<String>, MutableSet<FragmentToCheck>> = mutableMapOf()
        fragments.forEach {
            attributeSets.getOrPut(it.attributes, { hashSetOf() }).add(it)
        }
        val duplicateAttributesFragments = attributeSets.filter { it.value.size > 1 }
        return duplicateAttributesFragments
    }

    private fun expectedTransitiveRefinementEdges(
        fragments: Set<FragmentToCheck>
    ): Map<FragmentToCheck, Set<FragmentToCheck>> {
        val expectedRefinementEdges = mutableMapOf<FragmentToCheck, MutableSet<FragmentToCheck>>()
        fragments.forEach { leftFragment ->
            expectedRefinementEdges[leftFragment] = HashSet()
            fragments.forEach { rightFragment ->
                if (leftFragment.identifier != rightFragment.identifier && leftFragment.refines(rightFragment)) {
                    expectedRefinementEdges[leftFragment]!!.add(rightFragment)
                }
            }
        }
        return expectedRefinementEdges
    }

    private fun actualTransitiveRefinementEdges(
        fragments: Set<FragmentToCheck>,
        refinementEdges: Map<FragmentToCheck, Set<String>>,
        fragmentByIdentifier: Map<String, FragmentToCheck>,
    ): Map<FragmentToCheck, Set<FragmentToCheck>> {
        val refinementEdgesTransitiveClosure = mutableMapOf<FragmentToCheck, Set<FragmentToCheck>>()
        fun buildRefinementEdgesTransitiveClosure(fragment: FragmentToCheck): Set<FragmentToCheck> {
            refinementEdgesTransitiveClosure[fragment]?.let { return it }
            val edges = HashSet(refinementEdges[fragment]!!.map { fragmentByIdentifier[it]!! })
            refinementEdges[fragment]!!.forEach {
                edges.addAll(
                    buildRefinementEdgesTransitiveClosure(
                        fragmentByIdentifier[it]!!,
                    )
                )
            }
            refinementEdgesTransitiveClosure[fragment] = edges
            return edges
        }
        fragments.forEach { buildRefinementEdgesTransitiveClosure(it) }
        return refinementEdgesTransitiveClosure
    }

    private fun findFirstCycle(
        fragments: Iterable<FragmentToCheck>,
        refinementEdges: Map<FragmentToCheck, Set<String>>,
        fragmentByIdentifier: Map<String, FragmentToCheck>,
    ): Violation.FirstEncounteredCycle? {
        val cycleFreeFragments = mutableSetOf<FragmentToCheck>()
        fun dfs(
            fragment: FragmentToCheck,
            backtrace: HashSet<FragmentToCheck>
        ): List<FragmentToCheck>? {
            if (fragment in cycleFreeFragments) {
                return null
            }
            if (fragment in backtrace) {
                return backtrace.toList() + listOf(fragment)
            }
            backtrace.add(fragment)
            refinementEdges[fragment]!!.forEach {
                val cycle = dfs(
                    fragmentByIdentifier[it]!!,
                    backtrace,
                )
                if (cycle != null) {
                    return cycle
                }
            }
            backtrace.remove(fragment)
            // This fragment definitely doesn't lead to a cycle
            cycleFreeFragments.add(fragment)
            return null
        }
        fragments.forEach {
            val cycle = dfs(it, hashSetOf())
            if (cycle != null) return Violation.FirstEncounteredCycle(cycle)
        }
        return null
    }

    private fun checkAllRefineesHaveCorresponsingFragment(
        refinementEdges: Map<FragmentToCheck, Set<String>>,
        fragmentByIdentifier: Map<String, FragmentToCheck>,
    ): List<Violation> {
        val missingFragments = hashSetOf<String>()
        refinementEdges.values.forEach {
            it.forEach {
                // Check all vertices are provided
                if (fragmentByIdentifier[it] == null) {
                    missingFragments.add(it)
                }
            }
        }
        return missingFragments.map { Violation.MissingFragment(it) }
    }
}