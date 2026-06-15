// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY
// Reason: https://issuetracker.google.com/issues/524008575
// CURIOUS_ABOUT: writeToParcel
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Test(val names: List<List<ArrayList<String>>>): Parcelable
