package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
class A(val firstName: String) : Parcelable {
    val <warning descr="[PLUGIN_WARNING] Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning">secondName</warning>: String = ""

    val <warning descr="[PLUGIN_WARNING] Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning">delegated</warning> by lazy { "" }

    lateinit var <warning descr="[PLUGIN_WARNING] Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning">lateinit</warning>: String

    val customGetter: String
        get() = ""

    var customSetter: String
        get() = ""
        set(v) {}
}
