// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.nanoseconds

@Parcelize
data class Test(
    val basic: Duration,
    val negative: Duration,
    val nullable: Duration?,
    val nullableWithValue: Duration?,
    val noNanoseconds: Duration,
    val noSeconds: Duration,
    val infinite: Duration,
    val negativeInfinite: Duration,
) : Parcelable


fun box() = parcelTest { parcel ->
    val test = Test(
        basic = 10.seconds + 2.nanoseconds,
        negative = -10.seconds - 2.nanoseconds,
        nullable = null,
        nullableWithValue = 10.seconds + 2.nanoseconds,
        noNanoseconds = 10.seconds,
        noSeconds = 2.nanoseconds,
        infinite = Duration.INFINITE,
        negativeInfinite = -Duration.INFINITE,
    )
    test.writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    val got = parcelableCreator<Test>().createFromParcel(parcel)
    assert(test == got)
}
