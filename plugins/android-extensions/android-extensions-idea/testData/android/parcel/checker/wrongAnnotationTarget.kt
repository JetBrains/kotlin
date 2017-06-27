package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
interface <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">Intf</error> : Parcelable

@MagicParcel
object <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">Obj</error>

class A {
    @MagicParcel
    companion <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">object</error> {
        fun foo() {}
    }
}

@MagicParcel
enum class <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">Enum</error> {
    WHITE, BLACK
}

@MagicParcel
annotation class <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">Anno</error>