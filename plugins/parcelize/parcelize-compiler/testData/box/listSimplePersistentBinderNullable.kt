// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY
// Reason: https://issuetracker.google.com/issues/524008575
// WITH_STDLIB

@file:JvmName("TestKt")

package test

import kotlinx.parcelize.*
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import kotlinx.collections.immutable.*
import java.io.Serializable

class MockBinder(val id: Int) : Binder(), Serializable {
    override fun equals(other: Any?): Boolean = other is MockBinder && other.id == id
    override fun hashCode(): Int = id
}

@Parcelize
data class Test(val a: PersistentList<IBinder>?) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(null)
    val second = Test(persistentListOf(MockBinder(1), MockBinder(2)))

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = parcelableCreator<Test>().createFromParcel(parcel)
    val second2 = parcelableCreator<Test>().createFromParcel(parcel)

    assert(first == first2)
    assert(second == second2)
}
