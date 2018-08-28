package test

import kotlinx.android.parcel.*
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
        set(<warning descr="[UNUSED_PARAMETER] Parameter 'v' is never used">v</warning>) {}
}

@Parcelize
@Suppress("WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET")
class B(<warning descr="[PLUGIN_WARNING] '@IgnoredOnParcel' is inapplicable to properties declared in the primary constructor">@IgnoredOnParcel</warning> val firstName: String) : Parcelable {
    @IgnoredOnParcel
    var a: String = ""

    @field:IgnoredOnParcel
    var <warning descr="[PLUGIN_WARNING] Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning">b</warning>: String = ""

    @get:IgnoredOnParcel
    var c: String = ""

    @set:IgnoredOnParcel
    var <warning descr="[PLUGIN_WARNING] Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning">d</warning>: String = ""
}