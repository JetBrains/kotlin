// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_RUNTIME

import kotlinx.parcelize.*
import android.os.Parcelable
import java.io.Serializable

class SerializableSimple(val a: String, val b: String) : Serializable

@Parcelize
class User(val notNull: SerializableSimple, val nullable: SerializableSimple) : Parcelable