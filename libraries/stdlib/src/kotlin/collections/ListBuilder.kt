@file:UseExperimental(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

/**
 * Build a new [ArrayList] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Lists.buildArrayList
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> arrayList(
    @BuilderInference builderAction: ArrayList<E>.() -> Unit
): ArrayList<E> = ArrayList<E>().apply(builderAction)

/**
 * Build a new [ArrayList] with the given [initialCapacity] and [elements][E]
 * from the [builderAction].
 *
 * @sample samples.collections.Builders.Lists.buildArrayList
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> arrayList(
    initialCapacity: Int,
    @BuilderInference builderAction: ArrayList<E>.() -> Unit
): ArrayList<E> = ArrayList<E>(initialCapacity).apply(builderAction)

/**
 * Build a new [ArrayList] with the [elements][E] from the [builderAction]
 * that are not null.
 *
 * @sample samples.collections.Builders.Lists.buildArrayListOfNonNull
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E : Any> arrayListOfNonNull(
    @BuilderInference builderAction: ArrayList<E?>.() -> Unit
): ArrayList<E> = arrayList(builderAction).removeNull()

/**
 * Build a new [ArrayList] with the given [initialCapacity] and [elements][E]
 * from the [builderAction] that are not null.
 *
 * @sample samples.collections.Builders.Lists.buildArrayListOfNonNull
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E : Any> arrayListOfNonNull(
    initialCapacity: Int,
    @BuilderInference builderAction: ArrayList<E?>.() -> Unit
): ArrayList<E> = arrayList(initialCapacity, builderAction).removeNull()

/**
 * Build a new [MutableList] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Lists.buildMutableList
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> mutableList(
    @BuilderInference builderAction: MutableList<E>.() -> Unit
): MutableList<E> = arrayList(builderAction)

/**
 * Build a new [MutableList] with the given [initialCapacity] and [elements][E]
 * from the [builderAction].
 *
 * @sample samples.collections.Builders.Lists.buildMutableList
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> mutableList(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableList<E>.() -> Unit
): MutableList<E> = arrayList(initialCapacity, builderAction)

/**
 * Build a new [MutableList] with the [elements][E] from the [builderAction]
 * that are not null.
 *
 * @sample samples.collections.Builders.Lists.buildMutableListOfNonNull
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E : Any> mutableListOfNonNull(
    @BuilderInference builderAction: MutableList<E?>.() -> Unit
): MutableList<E> = arrayListOfNonNull(builderAction)

/**
 * Build a new [MutableList] with the given [initialCapacity] and [elements][E]
 * from the [builderAction] that are not null.
 *
 * @sample samples.collections.Builders.Lists.buildMutableListOfNonNull
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E : Any> mutableListOfNonNull(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableList<E?>.() -> Unit
): MutableList<E> = arrayListOfNonNull(initialCapacity, builderAction)

/**
 * Build a new read-only [List] with the [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Lists.buildList
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> list(
    @BuilderInference builderAction: MutableList<E>.() -> Unit
): List<E> = arrayList(builderAction)

/**
 * Build a new read-only [List] with the given [initialCapacity] and
 * [elements][E] from the [builderAction].
 *
 * @sample samples.collections.Builders.Lists.buildList
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E> list(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableList<E>.() -> Unit
): List<E> = arrayList(initialCapacity, builderAction)

/**
 * Build a new read-only [List] with the [elements][E] from the [builderAction]
 * that are not null.
 *
 * @sample samples.collections.Builders.Lists.buildListOfNonNull
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E : Any> listOfNonNull(
    @BuilderInference builderAction: MutableList<E?>.() -> Unit
): List<E> = arrayListOfNonNull(builderAction)

/**
 * Build a new read-only [List] with the given [initialCapacity] and
 * [elements][E] from the [builderAction] that are not null.
 *
 * @sample samples.collections.Builders.Lists.buildListOfNonNull
 * @throws IllegalArgumentException if the given [initialCapacity] is negative.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public inline fun <E : Any> listOfNonNull(
    initialCapacity: Int,
    @BuilderInference builderAction: MutableList<E?>.() -> Unit
): List<E> = arrayListOfNonNull(initialCapacity, builderAction)
