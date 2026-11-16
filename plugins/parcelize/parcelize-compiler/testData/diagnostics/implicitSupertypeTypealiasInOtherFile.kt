// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB
// The generated supertype must not cause SUPERTYPE_APPEARS_TWICE when Parcelable comes through a typealias in another file.

// FILE: alias.kt
package test

typealias AliasToParcelable = android.os.Parcelable

// FILE: main.kt
package test

import kotlinx.parcelize.Parcelize

@Parcelize
class User(val name: String) : AliasToParcelable
