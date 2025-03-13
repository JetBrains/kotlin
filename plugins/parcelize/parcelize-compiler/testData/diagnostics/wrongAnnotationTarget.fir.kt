package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
interface <!PARCELABLE_SHOULD_BE_CLASS!>Intf<!> : Parcelable

@Parcelize
object <!NO_PARCELABLE_SUPERTYPE!>Obj<!>

class A {
    @Parcelize
    companion <!NO_PARCELABLE_SUPERTYPE!>object<!> {
        fun foo() {}
    }
}

@Parcelize
enum class <!NO_PARCELABLE_SUPERTYPE!>Enum<!> {
    WHITE, BLACK
}

@Parcelize
annotation class <!ANNOTATION_CLASS_MEMBER, ANNOTATION_CLASS_MEMBER, PARCELABLE_SHOULD_BE_CLASS!>Anno<!>
