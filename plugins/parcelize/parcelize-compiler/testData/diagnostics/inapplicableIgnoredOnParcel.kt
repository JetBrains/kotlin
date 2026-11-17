// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: https://issuetracker.google.com/586858960
package test

import kotlinx.parcelize.*
import android.os.Parcelable

<!INAPPLICABLE_IGNORED_ON_PARCEL!>@IgnoredOnParcel<!>
val topLevel: String = ""

<!INAPPLICABLE_IGNORED_ON_PARCEL!>@IgnoredOnParcel<!>
var topLevelVar: String = ""

<!INAPPLICABLE_IGNORED_ON_PARCEL!>@IgnoredOnParcel<!>
val topLevelWithGetter: String
    get() = ""

<!INAPPLICABLE_IGNORED_ON_PARCEL!>@IgnoredOnParcel<!>
val String.topLevelExtension: String
    get() = this
