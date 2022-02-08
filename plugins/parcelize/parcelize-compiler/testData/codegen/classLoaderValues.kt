// CURIOUS_ABOUT: writeToParcel, createFromParcel
// WITH_STDLIB

// Test to ensure that we are using the correct class loader
// for generic types and bundles. See:
// - https://issuetracker.google.com/184072801
// - https://issuetracker.google.com/187577721

import kotlinx.parcelize.*
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle

@Parcelize
class A<T>(val a: @RawValue T, val b: Bundle, val c: PersistableBundle) : Parcelable
