// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY
// Reason: https://issuetracker.google.com/issues/524008575
// CURIOUS_ABOUT: createFromParcel
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable
import kotlinx.collections.immutable.*

@Parcelize
class Test(val names: PersistentList<String>): Parcelable
