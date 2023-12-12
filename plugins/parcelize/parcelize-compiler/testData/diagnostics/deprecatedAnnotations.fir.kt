// WITH_STDLIB
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

object Parceler1 : Parceler<String> {
    override fun create(parcel: Parcel) = parcel.readInt().toString()

    override fun String.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(length)
    }
}

object Parceler2 : Parceler<List<String>> {
    override fun create(parcel: Parcel) = listOf(parcel.readString()!!)

    override fun List<String>.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.joinToString(","))
    }
}

<!DEPRECATED_ANNOTATION!>@Parcelize<!>
<!FORBIDDEN_DEPRECATED_ANNOTATION!>@TypeParceler<String, Parceler2><!>
data class Test(
    val a: String,
    val b: <!FORBIDDEN_DEPRECATED_ANNOTATION!>@WriteWith<Parceler1><!> String,
    val c: <!FORBIDDEN_DEPRECATED_ANNOTATION!>@WriteWith<Parceler2><!> List<<!FORBIDDEN_DEPRECATED_ANNOTATION!>@WriteWith<Parceler1><!> String>
) : Parcelable {
    <!DEPRECATED_ANNOTATION!>@IgnoredOnParcel<!>
    val x by lazy { "foo" }
}

interface ParcelerForUser: Parceler<User>

<!DEPRECATED_ANNOTATION!>@Parcelize<!>
class User(val name: String) : Parcelable {
    private companion <!DEPRECATED_PARCELER!>object<!> : ParcelerForUser {
        override fun User.write(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
        }

        override fun create(parcel: Parcel) = User(parcel.readString()!!)
    }
}
