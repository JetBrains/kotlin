// CURIOUS_ABOUT <clinit>
// WITH_RUNTIME

import kotlinx.android.parcel.*
import android.os.Parcelable
import kotlin.jvm.JvmStatic

@Parcelize
class User(val firstName: String) : Parcelable {
    companion object {
        @JvmStatic
        private val test = StringBuilder()
    }
}