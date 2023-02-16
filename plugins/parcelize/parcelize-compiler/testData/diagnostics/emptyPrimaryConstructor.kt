// FIR_IDENTICAL
package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
class User : Parcelable

@Parcelize
class <!PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY!>User2<!>() : Parcelable
