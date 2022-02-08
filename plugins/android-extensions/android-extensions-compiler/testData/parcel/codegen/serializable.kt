// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_STDLIB

import kotlinx.android.parcel.*
import android.os.Parcelable
import java.io.Serializable

class SerializableSimple(val a: String, val b: String) : Serializable

@Parcelize
class User(val notNull: SerializableSimple, val nullable: SerializableSimple) : Parcelable