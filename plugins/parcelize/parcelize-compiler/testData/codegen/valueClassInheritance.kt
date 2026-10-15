// ISSUE: KT-89970
// CURIOUS_ABOUT: <init>
// WITH_STDLIB
// LANGUAGE: +FullValueClasses
// VALHALLA_VALUE_CLASSES
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// IGNORE_DEXING

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
sealed value class Base : Parcelable {
    abstract val id: Int
}

@Parcelize
value class Derived(override val id: Int, val s: String) : Base()
