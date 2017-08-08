// CURIOUS_ABOUT writeToParcel, createFromParcel

import kotlinx.android.parcel.*
import android.os.Parcelable
import android.os.IBinder
import android.os.IInterface

@Parcelize
class User(val binder: IBinder, val intf: IInterface?) : Parcelable