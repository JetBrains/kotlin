package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
class A(val firstName: String) : Parcelable {
    val <warning descr="[PROPERTY_WONT_BE_SERIALIZED] Property would not be serialized into a 'Parcel'. Add '@Transient' annotation to it">secondName</warning>: String = ""

    val <warning descr="[PROPERTY_WONT_BE_SERIALIZED] Property would not be serialized into a 'Parcel'. Add '@Transient' annotation to it">delegated</warning> by lazy { "" }

    lateinit var <warning descr="[PROPERTY_WONT_BE_SERIALIZED] Property would not be serialized into a 'Parcel'. Add '@Transient' annotation to it">lateinit</warning>: String

    val customGetter: String
        get() = ""

    var customSetter: String
        get() = ""
        set(v) {}
}