package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
open class Open(val foo: String) : Parcelable

@MagicParcel
class Final(val foo: String) : Parcelable

@MagicParcel
<error descr="[PARCELABLE_SHOULD_BE_INSTANTIABLE] 'Parcelable' should not be a 'sealed' or 'abstract' class">abstract</error> class Abstract(val foo: String) : Parcelable

@MagicParcel
<error descr="[PARCELABLE_SHOULD_BE_INSTANTIABLE] 'Parcelable' should not be a 'sealed' or 'abstract' class">sealed</error> class Sealed(val foo: String) : Parcelable {
    class X : Sealed("")
}

class Outer {
    @MagicParcel
    <error descr="[PARCELABLE_CANT_BE_INNER_CLASS] 'Parcelable' can't be an inner class">inner</error> class Inner(val foo: String) : Parcelable
}

fun foo() {
    @MagicParcel
    <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">object</error> : Parcelable {}

    @MagicParcel
    class <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype"><error descr="[PARCELABLE_CANT_BE_LOCAL_CLASS] 'Parcelable' can't be a local class">Local</error></error> {}
}