package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
open class Open(val foo: String) : Parcelable

@Parcelize
class Final(val foo: String) : Parcelable

@Parcelize
<error descr="[PLUGIN_ERROR] 'Parcelable' should not be a 'sealed' or 'abstract' class">abstract</error> class Abstract(val foo: String) : Parcelable

@Parcelize
<error descr="[PLUGIN_ERROR] 'Parcelable' should not be a 'sealed' or 'abstract' class">sealed</error> class Sealed(val foo: String) : Parcelable {
    class X : Sealed("")
}

class Outer {
    @Parcelize
    <error descr="[PLUGIN_ERROR] 'Parcelable' can't be an inner class">inner</error> class Inner(val foo: String) : Parcelable
}

fun foo() {
    @Parcelize
    <error descr="[PLUGIN_ERROR] 'Parcelable' can't be a local class">object</error> : Parcelable {}

    @Parcelize
    class <error descr="[PLUGIN_ERROR] 'Parcelable' can't be a local class"><error descr="[PLUGIN_ERROR] No 'Parcelable' supertype">Local</error></error> {}
}
