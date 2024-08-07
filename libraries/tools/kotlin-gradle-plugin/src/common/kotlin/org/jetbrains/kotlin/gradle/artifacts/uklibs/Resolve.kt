package org.jetbrains.kotlin

import org.jetbrains.kotlin.tooling.core.withClosure
import java.io.File
import kotlin.collections.ArrayDeque

/**
 * f1, f2, ... fN
 * ->
 * [
 *    (f1, f1), (f1, f2), ... (f1, fn),
 *    (f2, f1), (f2, f2), ... (f2, fn),
 *    (fn, f1), (fn, f2), ... (fn, fn),
 * ]
 * ->
 * 1. Filter p[0].canSee(p[1])
 * 2. Sort visible fragments by canSee and then by name
 */

// Compilation -> publishedFile: File
inline fun <Compilation, Target, reified SourceSet> transformKGPModelToUklibModel(
    moduleIdentifier: String,
    publishedCompilations: List<Compilation>,
    publishedArtifact: Compilation.() -> File,
    defaultSourceSet: Compilation.() -> SourceSet,
    target: Compilation.() -> Target,
    dependsOn: SourceSet.() -> Set<SourceSet>,
    identifier: SourceSet.() -> String,
): Uklib<Target> {
    /**
     * 1. No parts of the graph are isolated
     * 2. There are no cycles (this is already done by KGP)
     * 3. Everything is rooted in a single node (already checked in KGP)
     * 4. There are no fragments with duplicated attributes
     */
    // Assume we did check everything

    val sourceSetToTargets = mutableMapOf<SourceSet, MutableSet<Target>>()
    val fragmentToArtifact = mutableMapOf<String, File>()
    publishedCompilations.forEach { compilation ->
        val compilationTarget = compilation.target()
        compilation.defaultSourceSet().withClosure(dependsOn).forEach { sourceSet ->
            sourceSetToTargets.getOrPut(
                sourceSet, { mutableSetOf() }
            ).add(compilationTarget)
        }
        fragmentToArtifact[compilation.defaultSourceSet().identifier()] = compilation.publishedArtifact()
    }

    return Uklib(
        module = Module(
            identifier = moduleIdentifier,
            fragments = sourceSetToTargets.map { (sourceSet, targets) ->
                Fragment(
                    identifier = sourceSet.identifier(),
                    attributes = targets,
                )
            }.toSet()
        ),
        fragmentToArtifact = fragmentToArtifact
    )
}



fun <Target> resolveModuleFragmentClasspath(
    module: Module<Target>,
    dependencies: Set<Module<Target>>,
): Map<Fragment<Target>, List<Fragment<Target>>> {
    val moduleFragments = module.fragments.toList()
    val fragmentClasspath = mutableMapOf<Fragment<Target>, MutableList<Fragment<Target>>>()

    resolveFragmentRefinersWithinModule(moduleFragments).forEach { (fragment, refiners) ->
        fragmentClasspath.getOrPut(fragment, { mutableListOf() }).addAll(refiners)
    }

    dependencies.forEach { dependency ->
        resolveFragmentDependencies(
            targetFragments = moduleFragments,
            dependencyFragments = dependency.fragments.toList(),
        ).forEach { (fragment, dependencies) ->
            (fragmentClasspath[fragment] ?: error("??")).addAll(dependencies)
        }
    }

    return fragmentClasspath
}

/**
 * This function takes fragments of a module [moduleFragments] and outputs a map where the keys are [moduleFragments]
 * and the values are classpath-ordered refiners.
 */
fun <Target> resolveFragmentRefinersWithinModule(
    moduleFragments: List<Fragment<Target>>,
) = resolveFragmentDependencies(
    targetFragments = moduleFragments,
    dependencyFragments = moduleFragments,
    canSee = { attributes.isProperSubsetOf(it.attributes) }
)

/**
 * This function takes fragments [targetFragments] that depend on another module's fragments [dependencyFragments] and
 * outputs a map where the keys are [targetFragments] and the values are classpath-ordered [dependencyFragments] visible
 * from the target fragment
 */
fun <Target> resolveFragmentDependencies(
    targetFragments: List<Fragment<Target>>,
    dependencyFragments: List<Fragment<Target>>,
) = resolveFragmentDependencies(
    targetFragments = targetFragments,
    dependencyFragments = dependencyFragments,
    canSee = { attributes.isSubsetOf(it.attributes) }
)

fun <Target> resolveFragmentDependencies(
    targetFragments: List<Fragment<Target>>,
    dependencyFragments: List<Fragment<Target>>,
    canSee: Fragment<Target>.(Fragment<Target>) -> Boolean,
): Map<Fragment<Target>, List<Fragment<Target>>> {
    val modulesFragmentClasspath = mutableMapOf<Fragment<Target>, List<Fragment<Target>>>()
    targetFragments.forEach { fragment ->
        val fragmentClasspath = dependencyFragments.filter {
            fragment.canSee(it)
        }.sortedWith(
            object : Comparator<Fragment<Target>> {
                override fun compare(left: Fragment<Target>, right: Fragment<Target>): Int {
                    if (left.canSee(right)) {
                        return -1
                    } else if (right.canSee(left)) {
                        return 1
                    } else if (left == right) {
                        return 0
                    } else {
                        /**
                         * The target fragment can see left and right, but the fragments are incomparable. For example in:
                         * ab; bc; b -> ab; b -> bc;
                         * b can see ab and bc, but ab and bc are incomparable
                         */
                        return left.identifier.compareTo(right.identifier)
                    }
                }
            }
        )
        modulesFragmentClasspath[fragment] = fragmentClasspath
    }
    return modulesFragmentClasspath
}



data class Module<Target>(
    // Do we even need this identifier
    val identifier: String,
    val fragments: Set<Fragment<Target>>,
) {
    override fun hashCode(): Int = identifier.hashCode()
    override fun equals(other: Any?): Boolean = identifier.equals(other?.toString())
}

data class Fragment<Target>(
    val identifier: String,
    val attributes: Set<Target>,
) {
    override fun hashCode(): Int = identifier.hashCode()
    override fun equals(other: Any?): Boolean = identifier.equals(other?.toString())
}

fun <E> Set<E>.isProperSubsetOf(another: Set<E>): Boolean = another.size > size && isSubsetOf(another)
fun <E> Set<E>.isSubsetOf(another: Set<E>): Boolean = another.containsAll(this)