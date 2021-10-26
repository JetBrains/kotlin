package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
interface <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">Intf</error> : Parcelable

@Parcelize
object <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype">Obj</error>

class A {
    @Parcelize
    companion <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype">object</error> {
        fun foo() {}
    }
}

@Parcelize
enum class <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype">Enum</error> {
    WHITE, BLACK
}

@Parcelize
annotation class <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">Anno</error>
