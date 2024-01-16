package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
interface <!PARCELABLE_SHOULD_BE_CLASS!>Intf<!> : Parcelable

@Parcelize
object <!NOTHING_TO_OVERRIDE, NOTHING_TO_OVERRIDE, NO_PARCELABLE_SUPERTYPE!>Obj<!>

class A {
    @Parcelize
    companion <!NOTHING_TO_OVERRIDE, NOTHING_TO_OVERRIDE, NO_PARCELABLE_SUPERTYPE!>object<!> {
        fun foo() {}
    }
}

@Parcelize
enum class <!NOTHING_TO_OVERRIDE, NOTHING_TO_OVERRIDE, NO_PARCELABLE_SUPERTYPE!>Enum<!> {
    WHITE, BLACK
}

@Parcelize
annotation class <!NOTHING_TO_OVERRIDE, NOTHING_TO_OVERRIDE, PARCELABLE_SHOULD_BE_CLASS!>Anno<!>
