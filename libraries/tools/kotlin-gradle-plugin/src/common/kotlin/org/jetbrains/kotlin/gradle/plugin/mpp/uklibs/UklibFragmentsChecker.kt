/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import java.io.Serializable

internal object UklibFragmentsChecker {
    data class FragmentToCheck(
        val identifier: String,
        val attributes: Set<String>,
    ) : Serializable {
        // Here refines means refines within the same Uklib module (i.e. source set dependsOn)
        fun refines(fragment: FragmentToCheck): Boolean = attributes.isSubsetOf(fragment.attributes)
    }

    sealed class Violation : Serializable {
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
        val missingFragments = hashSetOf<String>()
        refinementEdges.values.forEach {
            it.forEach {
                // Check all vertices are provided
                if (fragmentByIdentifier[it] == null) {
                    missingFragments.add(it)
                }
            }
        }
        if (missingFragments.isNotEmpty()) {
            violations.addAll(
                missingFragments.map {
                    Violation.MissingFragment(it)
                }
            )
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
            return violations
        }

        /**
         * Detect cycles to make sure we are working with a DAG
         *
         * Find and return the first encountered cycle similar to what we do in CircularDependsOnEdges
         */
        val cycleFreeFragments = mutableSetOf<FragmentToCheck>()
        fun findFirstCycle(
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
                findFirstCycle(
                    fragmentByIdentifier[it]!!,
                    backtrace,
                )?.let {
                    return it
                }
            }
            backtrace.remove(fragment)
            // This fragment definitely doesn't lead to a cycle
            cycleFreeFragments.add(fragment)
            return null
        }
        fragments.forEach {
            findFirstCycle(it, hashSetOf())?.let {
                violations.add(Violation.FirstEncounteredCycle(it))
                return violations
            }
        }

        /**
         * Violations of "It's forbidden to have several fragments with the same attributes in uklib"
         *
         * i.e. detect multiple-same targets and bamboos
         */
        val attributeSets: MutableMap<Set<String>, MutableSet<FragmentToCheck>> = mutableMapOf()
        fragments.forEach {
            attributeSets.getOrPut(it.attributes, { hashSetOf() }).add(it)
        }
        val duplicateAttributesFragments = attributeSets.filter { it.value.size > 1 }
        duplicateAttributesFragments.forEach {
            violations.add(
                Violation.DuplicateAttributesFragments(
                    it.key,
                    it.value
                )
            )
        }
        val allDuplicateAttributesFragments = duplicateAttributesFragments.values.flatten()

        /**
         * Violations of "Fragment `F1` refines fragment `F2` <=> targets of `F1` are compatible with `F2`"
         *
         * 1. For each fragment using the attributes find which other fragments it must refine
         */
        val expectedRefinementEdges = mutableMapOf<FragmentToCheck, MutableSet<FragmentToCheck>>()
        fragments.forEach { leftFragment ->
            expectedRefinementEdges[leftFragment] = HashSet()
            fragments.forEach { rightFragment ->
                if (leftFragment.identifier != rightFragment.identifier && leftFragment.refines(rightFragment)) {
                    expectedRefinementEdges[leftFragment]!!.add(rightFragment)
                }
            }
        }

        // Transitive closure of fragment refinees
        val refinementEdgesTransitiveClosure: MutableMap<FragmentToCheck, Set<String>> = mutableMapOf()
        fun buildRefinementEdgesTransitiveClosure(fragment: FragmentToCheck): Set<String> {
            refinementEdgesTransitiveClosure[fragment]?.let { return it }
            val edges = HashSet<String>(refinementEdges[fragment]!!)
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

        fragments.forEach { fragment ->
            val actuallyRefinedFragments = refinementEdgesTransitiveClosure[fragment]!!.map {
                fragmentByIdentifier[it]!!
            }.toHashSet()
            val expectedRefinementFragments = expectedRefinementEdges[fragment]!!

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
         * Left to right is explicitly checked here. Right to left is checked by multiple-same targets and [incompatibleRefinementViolations]
         */
        val orphanedIntermediateFragments = fragments.filter { it.attributes.size > 1 }.toHashSet()
        orphanedIntermediateFragments.removeAll(
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
}