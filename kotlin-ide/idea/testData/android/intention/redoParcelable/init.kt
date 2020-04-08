// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.RedoParcelableAction

import android.os.Parcel
import android.os.Parcelable


class <caret>MyData(parcel: Parcel) : Parcelable {

    var count: Int = 0
    var text: String = ""

    init {
        count = parcel.readInt()
        text = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(count)
        parcel.writeString(text)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MyData> {
        override fun createFromParcel(parcel: Parcel): MyData {
            return MyData(parcel)
        }

        override fun newArray(size: Int): Array<MyData?> {
            return arrayOfNulls(size)
        }
    }
}