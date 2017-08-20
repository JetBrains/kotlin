package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
open class Open(val foo: String) : Parcelable

@Parcelize
class Final(val foo: String) : Parcelable

@Parcelize
<error descr="[PARCELABLE_SHOULD_BE_INSTANTIABLE] 'Parcelable' should not be a 'sealed' or 'abstract' class">abstract</error> class Abstract(val foo: String) : Parcelable

@Parcelize
<error descr="[PARCELABLE_SHOULD_BE_INSTANTIABLE] 'Parcelable' should not be a 'sealed' or 'abstract' class">sealed</error> class Sealed(val foo: String) : Parcelable {
    class X : Sealed("")
}

class Outer {
    @Parcelize
    <error descr="[PARCELABLE_CANT_BE_INNER_CLASS] 'Parcelable' can't be an inner class">inner</error> class Inner(val foo: String) : Parcelable
}

fun foo() {
    @Parcelize
    <error descr="[PARCELABLE_SHOULD_BE_CLASS] 'Parcelable' should be a class">object</error> : Parcelable {}

    @Parcelize
    class <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype"><error descr="[PARCELABLE_CANT_BE_LOCAL_CLASS] 'Parcelable' can't be a local class">Local</error></error> {}
}