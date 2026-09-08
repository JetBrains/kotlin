// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

const val TAG_SUCCESS = 100
const val TAG_FAILED = -1

@Parcelize
@PolymorphicSealed
sealed class CustomTagState : Parcelable {
    @ParcelTag(0)
    object Initial : CustomTagState()

    @ParcelTag(TAG_SUCCESS + 1)
    data class Success(val value: Int) : CustomTagState()

    @ParcelTag(TAG_FAILED)
    class Failure(val reason: String) : CustomTagState() {
        override fun equals(other: Any?): Boolean = other is Failure && reason == other.reason
    }

    @ParcelTag(42)
    data class Custom(val x: Double) : CustomTagState()
}

fun box() = parcelTest { parcel ->
    val states = listOf(
        CustomTagState.Initial,
        CustomTagState.Success(123),
        CustomTagState.Failure("Timeout"),
        CustomTagState.Custom(3.14),
    )

    for (state in states) {
        state.writeToParcel(parcel, 0)
    }

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    for (state in states) {
        assert(state == parcelableCreator<CustomTagState>().createFromParcel(parcel))
    }
}
