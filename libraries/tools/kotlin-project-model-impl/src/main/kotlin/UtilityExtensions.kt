package org.jetbrains.kotlin.project.modelx


/**
 * Calculate refinement closure of given [fragmentId]
 * set [includeStart] to include starting [fragmentId] into closure
 */
fun KotlinModule.refinementClosure(fragmentId: FragmentId, includeStart: Boolean = true): Set<Fragment> {
    val start = fragments[fragmentId] ?: error("Fragment not found: $fragmentId")
    val result = if (includeStart) mutableSetOf(start) else mutableSetOf()

    var nextIds = refinements[fragmentId] ?: setOf()
    while (nextIds.isNotEmpty()) {
        val ids = mutableSetOf<FragmentId>()
        nextIds.flatMapTo(ids) { refinements[it] ?: emptySet() }
        nextIds.mapTo(result) { fragments[it] ?: error("Fragment not found $it") }
        nextIds = ids
    }

    return result
}

/**
 * Iterate refinement tree (or forest) from root(s) till leaves level (same distance from root) by level
 * just like breadth-first iterator
 *
 * pro-tip: call `module.iterateRefinementTree().flatten()` when you don't need levels itself but just breadth-first iteration order
 */
fun KotlinModule.iterateRefinementTree(): Sequence<Set<Fragment>> {
    val startingFragments = fragments.keys - refinements.filterValues { it.isNotEmpty() }.keys
    return iterateRefinementTree(startingFragments)
}

/**
 * from top to bottom
 */
fun KotlinModule.iterateRefinementTree(fromFragment: FragmentId): Sequence<Set<Fragment>> = iterateRefinementTree(setOf(fromFragment))

/**
 * from top to bottom
 */
private fun KotlinModule.iterateRefinementTree(startingFragments: Set<FragmentId>): Sequence<Set<Fragment>> = sequence {
    var pool = startingFragments
    while (pool.isNotEmpty()) {
        yield(pool.map { fragments[it] ?: error("Fragment not found $it") }.toSet())
        pool = pool.flatMap { reverseRefinementsMap[it] ?: emptySet() }.toSet()
    }
}

/**
 * Returns a set of [Variant]s which refinement closure contains the given [fragmentId]
 * In other words the given [fragmentId] participates in building each of returned [Variant]
 *
 * Invariant: `refiningVariants(fragmentId).all { variant -> fragmentId in refinementClosure(variant.id) }`
 */
fun KotlinModule.refiningVariants(fragmentId: FragmentId): Set<Variant> =
    iterateRefinementTree(fragmentId)
        .flatten()
        .filterIsInstance<Variant>()
        .toSet()

/**
 * Infers Kotlin Attributes for a given [fragmentId]
 */
fun KotlinModule.fragmentAttributes(fragmentId: FragmentId): Map<Attribute.Key, Attribute> {
    // Fast return if given [fragmentId] is in fact an Instance of Variant
    (fragments[fragmentId] as? Variant)?.also { return it.attributes }

    val refiningVariants = refiningVariants(fragmentId)
    val result = mutableMapOf<Attribute.Key, Attribute>()

    // TODO: Fragment Attributes Inferring algorithm?
    // refiningVariants.intersectAttributes() ?

    return emptyMap()
}

fun KotlinModule.fragmentPlatforms(fragmentId: FragmentId): Set<Platform> = this
    .refiningVariants(fragmentId)
    .map { it.platform }
    .toSet()

/**
 * Check isCompatible relation of two fragments based on Attribute compatibility
 */
infix fun Variant.isCompatible(that: Variant): Boolean {
    val keys = this.attributes.keys + that.attributes.keys

    return keys.all { key ->
        val leftValue = this.attributes[key]
        val rightValue = that.attributes[key]

        key.isCompatible(leftValue, rightValue)
            .also { println("Attribute: $key isCompatible($leftValue, $rightValue) == $it") }

    }
}
