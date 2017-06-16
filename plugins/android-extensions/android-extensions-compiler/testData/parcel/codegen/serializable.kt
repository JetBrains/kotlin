// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>

import kotlinx.android.parcel.*
import android.os.Parcelable
import java.io.Serializable

class SerializableSimple(val a: String, val b: String) : Serializable

@MagicParcel
class User(val notNull: SerializableSimple, val nullable: SerializableSimple) : Parcelable