// CURIOUS_ABOUT writeToParcel, createFromParcel
// WITH_RUNTIME

import kotlinx.android.parcel.*
import android.os.Parcelable
import android.os.IBinder
import android.os.IInterface

@Parcelize
class User(
        val binder: IBinder,
        val binderArray: Array<IBinder>,
        val binderList: List<IBinder>,
        val binderArrayList: ArrayList<IBinder>, // should be serialized using our strategy, not using Parcel.writeBinderList()
        val intf: IInterface?
) : Parcelable