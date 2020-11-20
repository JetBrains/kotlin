// WITH_RUNTIME
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

<warning descr="[DEPRECATED_ANNOTATION] Parcelize annotations from package 'kotlinx.android.parcel' are deprecated. Change package to 'kotlin.parcelize'">@Parcelize</warning>
<error descr="[FORBIDDEN_DEPRECATED_ANNOTATION] Parceler-related annotations from package 'kotlinx.android.parcel' are forbidden. Change package to 'kotlinx.parcelize'">@TypeParceler<String, <error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'Parceler<in String>'">Parceler2</error>></error>
data class Test(
    val a: String,
    val b: <error descr="[FORBIDDEN_DEPRECATED_ANNOTATION] Parceler-related annotations from package 'kotlinx.android.parcel' are forbidden. Change package to 'kotlinx.parcelize'">@WriteWith<Parceler1></error> String,
    val c: <error descr="[FORBIDDEN_DEPRECATED_ANNOTATION] Parceler-related annotations from package 'kotlinx.android.parcel' are forbidden. Change package to 'kotlinx.parcelize'">@WriteWith<Parceler2></error> List<<error descr="[FORBIDDEN_DEPRECATED_ANNOTATION] Parceler-related annotations from package 'kotlinx.android.parcel' are forbidden. Change package to 'kotlinx.parcelize'">@WriteWith<Parceler1></error> String>
) : Parcelable {
    <warning descr="[DEPRECATED_ANNOTATION] Parcelize annotations from package 'kotlinx.android.parcel' are deprecated. Change package to 'kotlin.parcelize'">@IgnoredOnParcel</warning>
    val x by lazy { "foo" }
}

interface ParcelerForUser: Parceler<User>

<warning descr="[DEPRECATED_ANNOTATION] Parcelize annotations from package 'kotlinx.android.parcel' are deprecated. Change package to 'kotlin.parcelize'">@Parcelize</warning>
class User(val name: String) : Parcelable {
    private companion <error descr="[DEPRECATED_PARCELER] 'kotlinx.android.parcel.Parceler' is deprecated. Use 'kotlinx.parcelize.Parceler' instead">object</error> : ParcelerForUser {
        override fun User.write(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
        }

        override fun create(parcel: Parcel) = User(parcel.readString()!!)
    }
}