// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

/**
 * Generic pair parceler
 * Create concrete object to use (see below)
 */
open class PairParceler<F: Any, S: Any>(private val firstParceler: Parceler<F>, private val secondParceler: Parceler<S>): Parceler<Pair<F, S>> {
    /**
     * Reads the [T] instance state from the [parcel], constructs the new [T] instance and returns it.
     */
    override fun create(parcel: Parcel): Pair<F, S> =
        firstParceler.create(parcel) to secondParceler.create(parcel)

    /**
     * Writes the [T] instance state to the [parcel].
     */
    override fun Pair<F, S>.write(parcel: Parcel, flags: Int) {
        with(firstParceler) { this@write.first.write(parcel, 0) }
        with(secondParceler) { this@write.second.write(parcel, 0) }
    }
}

object IntParceler: Parceler<Int> {
    /**
     * Reads the [T] instance state from the [parcel], constructs the new [T] instance and returns it.
     */
    override fun create(parcel: Parcel): Int = parcel.readInt()

    /**
     * Writes the [T] instance state to the [parcel].
     */
    override fun Int.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(this)
    }
}

/**
 * [Int] to [Int] pair parceler
 */
object IntToIntParceler: PairParceler<Int, Int>(IntParceler, IntParceler)

@Parcelize
@TypeParceler<Pair<Int, Int>, IntToIntParceler>
class A(val pair: Pair<Int, Int>): Parcelable

fun box() = parcelTest { parcel ->
    val a1 = A(1 to 2)
    a1.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val a2 = parcelableCreator<A>().createFromParcel(parcel)
    assert(a1.pair == a2.pair)
}
