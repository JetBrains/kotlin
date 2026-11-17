// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// https://issuetracker.google.com/585078316 Parcelize: compiler crash on @IgnoredOnParcel constructor property without default value

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class B(<!INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY!>@IgnoredOnParcel<!> val firstName: String) : Parcelable
