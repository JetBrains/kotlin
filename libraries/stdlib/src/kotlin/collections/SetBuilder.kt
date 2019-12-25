@file:UseExperimental(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

/**
 * Build a new [HashSet] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildHashSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> hashSet(
    @BuilderInference builderAction: HashSet<E>.() -> Unit
): HashSet<E> = HashSet<E>().apply(builderAction)

/**
 * Build a new [HashSet] with the given [initialCapacity] and [elements][E] from
 * the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildHashSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> hashSet(
    initialCapacity: Int,
    @BuilderInference builderAction: HashSet<E>.() -> Unit
): HashSet<E> = HashSet<E>(initialCapacity).apply(builderAction)

/**
 * Build a new [HashSet] with the given [initialCapacity], [loadFactor], and the
 * [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildHashSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> hashSet(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: HashSet<E>.() -> Unit
): HashSet<E> = HashSet<E>(initialCapacity, loadFactor).apply(builderAction)

/**
 * Build a new [LinkedHashSet] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildLinkedSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> linkedSet(
    @BuilderInference builderAction: LinkedHashSet<E>.() -> Unit
): LinkedHashSet<E> = LinkedHashSet<E>().apply(builderAction)

/**
 * Build a new [LinkedHashSet] with the given [initialCapacity] and
 * [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildLinkedSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> linkedSet(
    initialCapacity: Int,
    @BuilderInference builderAction: LinkedHashSet<E>.() -> Unit
): LinkedHashSet<E> = LinkedHashSet<E>(initialCapacity).apply(builderAction)

/**
 * Build a new [LinkedHashSet] with the given [initialCapacity], [loadFactor],
 * and the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildLinkedSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> linkedSet(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: LinkedHashSet<E>.() -> Unit
): LinkedHashSet<E> = LinkedHashSet<E>(initialCapacity, loadFactor).apply(builderAction)

/**
 * Build a new [MutableSet] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildMutableSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> mutableSet(
    @BuilderInference builderAction: MutableSet<E>.() -> Unit
): MutableSet<E> = linkedSet(builderAction)

/**
 * Build a new [MutableSet] with the given [initialCapacity] and [elements][E]
 * from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildMutableSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> mutableSet(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableSet<E>.() -> Unit
): MutableSet<E> = linkedSet(initialCapacity, builderAction)

/**
 * Build a new [MutableSet] with the given [initialCapacity], [loadFactor], and
 * the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildMutableSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> mutableSet(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: MutableSet<E>.() -> Unit
): MutableSet<E> = linkedSet(initialCapacity, loadFactor, builderAction)

/**
 * Build a new read-only [Set] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSet
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> set(
    @BuilderInference builderAction: MutableSet<E>.() -> Unit
): Set<E> = linkedSet(builderAction)

/**
 * Build a new read-only [Set] with the given [initialCapacity] and
 * [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> set(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableSet<E>.() -> Unit
): Set<E> = linkedSet(initialCapacity, builderAction)

/**
 * Build a new read-only [Set] with the given [initialCapacity], [loadFactor],
 * and the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Sets.buildSet
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> set(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: MutableSet<E>.() -> Unit
): Set<E> = linkedSet(initialCapacity, loadFactor, builderAction)
