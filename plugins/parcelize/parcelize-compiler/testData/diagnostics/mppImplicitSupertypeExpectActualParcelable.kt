// LANGUAGE: +MultiPlatformProjects
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB
// The generated supertype must not cause SUPERTYPE_APPEARS_TWICE when Parcelable comes through an expect interface.

// MODULE: m1-common
// FILE: common.kt
package test

import kotlinx.parcelize.Parcelize

expect interface CustomParcelable

@Parcelize
class User(val name: String) : CustomParcelable

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt
package test

actual typealias CustomParcelable = android.os.Parcelable
