// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

@Parcelize
data class Film(val genres: Array<Int>) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Film

        if (!Arrays.equals(genres, other.genres)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(genres)
    }
}

fun box() = parcelTest { parcel ->
    val film = Film(arrayOf(3, 5, 7))
    film.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val film2 = readFromParcel<Film>(parcel)
    assert(film == film2)
}