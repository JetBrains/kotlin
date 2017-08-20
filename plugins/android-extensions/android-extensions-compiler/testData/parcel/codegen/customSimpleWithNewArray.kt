// CURIOUS_ABOUT newArray

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@MagicParcel
class User(val firstName: String, val lastName: String, val age: Int) : Parcelable {
    private companion object : Parceler<User> {
        override fun User.write(parcel: Parcel, flags: Int) {
            parcel.writeString(firstName)
            parcel.writeString(lastName)
            parcel.writeInt(age)
        }

        override fun create(parcel: Parcel) = User(parcel.readString(), parcel.readString(), parcel.readInt())

        override fun newArray(size: Int) = arrayOfNulls<User>(size) as Array<User>
    }
}