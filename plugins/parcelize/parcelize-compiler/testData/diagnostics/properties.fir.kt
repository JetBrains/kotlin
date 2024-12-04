// WITH_STDLIB
package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class A(val firstName: String) : Parcelable {
    val <!PROPERTY_WONT_BE_SERIALIZED!>secondName<!>: String = ""

    val <!PROPERTY_WONT_BE_SERIALIZED!>delegated<!> by lazy { "" }

    lateinit var <!PROPERTY_WONT_BE_SERIALIZED!>lateinit<!>: String

    val customGetter: String
        get() = ""

    var customSetter: String
        get() = ""
        set(v) {}
}

@Parcelize
@Suppress(<!ERROR_SUPPRESSION!>"WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET"<!>)
class B(<!INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY!>@IgnoredOnParcel<!> val firstName: String) : Parcelable {
    @IgnoredOnParcel
    var a: String = ""

    @field:IgnoredOnParcel
    var <!PROPERTY_WONT_BE_SERIALIZED!>b<!>: String = ""

    @get:IgnoredOnParcel
    var c: String = ""

    @set:IgnoredOnParcel
    var <!PROPERTY_WONT_BE_SERIALIZED!>d<!>: String = ""
}
