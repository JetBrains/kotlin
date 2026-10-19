package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
interface <!PARCELABLE_CANT_BE_NON_SEALED_INTERFACE!>Intf<!> : Parcelable

@Parcelize
object Obj

class A {
    @Parcelize
    companion object {
        fun foo() {}
    }
}

@Parcelize
enum class Enum {
    WHITE, BLACK
}

@Parcelize
annotation class <!ANNOTATION_CLASS_MEMBER, ANNOTATION_CLASS_MEMBER, PARCELABLE_CANT_BE_ANNOTATION_CLASS!>Anno<!>
