// IGNORE_BACKEND: JVM
// See KT-38103
// There is no such thing as a readStrongInterface method to deserialize arbitrary IIinterface implementations
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MockBinder : Binder(), Serializable

@Parcelize
class MockIInterface : IInterface, Parcelable {
    override fun asBinder(): IBinder = MockBinder()
}

@Parcelize
class ServiceContainer(
    val binder: MockBinder,
    val iinterface: MockIInterface,
    val binderArray: Array<IBinder>,
    val binderList: List<IBinder>
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = ServiceContainer(MockBinder(), MockIInterface(), arrayOf(MockBinder()), listOf(MockBinder()))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    parcelableCreator<ServiceContainer>().createFromParcel(parcel)
}
