@file:UseExperimental(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

/**
 * Build new [HashMap] with the [key][K]–[value][V] pairs from the
 * [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildHashMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> hashMap(
    @BuilderInference builderAction: HashMap<K, V>.() -> Unit
): HashMap<K, V> = HashMap<K, V>().apply(builderAction)

/**
 * Build new [HashMap] with the given [initialCapacity] and [key][K]–[value][V]
 * pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildHashMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> hashMap(
    initialCapacity: Int,
    @BuilderInference builderAction: HashMap<K, V>.() -> Unit
): HashMap<K, V> = HashMap<K, V>(initialCapacity).apply(builderAction)

/**
 * Build a new [HashMap] with the given [initialCapacity], [loadFactor], and the
 * [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildHashMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> hashMap(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: HashMap<K, V>.() -> Unit
): HashMap<K, V> = HashMap<K, V>(initialCapacity, loadFactor).apply(builderAction)

/**
 * Build a new [LinkedHashMap] with the [key][K]–[value][V] pairs from the
 * [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildLinkedMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> linkedMap(
    @BuilderInference builderAction: LinkedHashMap<K, V>.() -> Unit
): LinkedHashMap<K, V> = LinkedHashMap<K, V>().apply(builderAction)

/**
 * Build a new [LinkedHashMap] with the given [initialCapacity] and
 * [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildLinkedMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> linkedMap(
    initialCapacity: Int,
    @BuilderInference builderAction: LinkedHashMap<K, V>.() -> Unit
): LinkedHashMap<K, V> = LinkedHashMap<K, V>(initialCapacity).apply(builderAction)

/**
 * Build a new [LinkedHashMap] with the given [initialCapacity], [loadFactor],
 * and the [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildLinkedMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> linkedMap(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: LinkedHashMap<K, V>.() -> Unit
): LinkedHashMap<K, V> = LinkedHashMap<K, V>(initialCapacity, loadFactor).apply(builderAction)

/**
 * Build a new [MutableMap] with the [key][K]–[value][V] pairs from the
 * [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildMutableMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> mutableMap(
    @BuilderInference builderAction: MutableMap<K, V>.() -> Unit
): MutableMap<K, V> = linkedMap(builderAction)

/**
 * Build a new [MutableMap] with the given [initialCapacity] and
 * [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildMutableMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> mutableMap(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableMap<K, V>.() -> Unit
): MutableMap<K, V> = linkedMap(initialCapacity, builderAction)

/**
 * Build a new [MutableMap] with the given [initialCapacity], [loadFactor], and
 * the [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildMutableMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> mutableMap(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: MutableMap<K, V>.() -> Unit
): MutableMap<K, V> = linkedMap(initialCapacity, loadFactor, builderAction)

/**
 * Build a new read-only [Map] with the [key][K]–[value][V] pairs from the
 * [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildMap
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> map(
    @BuilderInference builderAction: MutableMap<K, V>.() -> Unit
): Map<K, V> = linkedMap(builderAction)

/**
 * Build a new read-only [Map] with the given [initialCapacity] and
 * [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> map(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableMap<K, V>.() -> Unit
): Map<K, V> = linkedMap(initialCapacity, builderAction)

/**
 * Build a new read-only [Map] with the given [initialCapacity], [loadFactor],
 * and the [key][K]–[value][V] pairs from the [builderAction].
 *
 * @sample samples.collections.Builders.Maps.buildMap
 * @throws IllegalArgumentException if the given [initialCapacity] is negative
 *   or the given [loadFactor] is less than or equal to zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <K, V> map(
    initialCapacity: Int,
    loadFactor: Float,
    @BuilderInference builderAction: MutableMap<K, V>.() -> Unit
): Map<K, V> = linkedMap(initialCapacity, loadFactor, builderAction)
