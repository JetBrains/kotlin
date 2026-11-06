package test

import kotlinx.parcelize.AddParcelableSupertype
import kotlinx.parcelize.Parcelize
import android.os.Parcel
import android.os.Parcelable

@AddParcelableSupertype
@Parcelize
class WithParcelize(val x: Int)

@AddParcelableSupertype
interface Intf

@Parcelize
class Impl: Intf

@AddParcelableSupertype
sealed interface SealedIntf

@AddParcelableSupertype
abstract class AbstractClass

@AddParcelableSupertype
object Obj {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}

@AddParcelableSupertype
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class NotImplemented<!>

<!INAPPLICABLE_ADD_PARCELABLE_SUPERTYPE!>@AddParcelableSupertype<!>
annotation class Annotation

val anonymous = <!INAPPLICABLE_ADD_PARCELABLE_SUPERTYPE!>@AddParcelableSupertype<!> object {}
