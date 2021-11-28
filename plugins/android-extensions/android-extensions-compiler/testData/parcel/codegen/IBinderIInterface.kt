// CURIOUS_ABOUT writeToParcel, createFromParcel
// WITH_STDLIB

import kotlinx.android.parcel.*
import android.os.Parcelable
import android.os.IBinder
import android.os.IInterface

@Parcelize
class User(
        val binder: IBinder,
        val binderArray: Array<IBinder>,
        val binderList: List<IBinder>,
        val binderArrayList: ArrayList<IBinder> // should be serialized using our strategy, not using Parcel.writeBinderList()
        // There is no readStrongInterface method in Parcel.
        // val intf: IInterface?
) : Parcelable