package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
open class Open(val foo: String) : Parcelable

@Parcelize
class Final(val foo: String) : Parcelable

@Parcelize
<!PARCELABLE_SHOULD_BE_INSTANTIABLE!>abstract<!> class Abstract(val foo: String) : Parcelable

@Parcelize
sealed class Sealed(val foo: String) : Parcelable {
    class X : Sealed("")
    sealed class Inner : Sealed("") {
        <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Y<!> : Inner()
    }
}

class Outer {
    @Parcelize
    <!PARCELABLE_CANT_BE_INNER_CLASS!>inner<!> class Inner(val foo: String) : Parcelable
}

fun foo() {
    @Parcelize
    <!PARCELABLE_CANT_BE_LOCAL_CLASS!>object<!> : Parcelable {}

    @Parcelize
    class <!NO_PARCELABLE_SUPERTYPE, PARCELABLE_CANT_BE_LOCAL_CLASS!>Local<!> {}
}
