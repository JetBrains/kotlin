// LANGUAGE: +CompanionBlocks
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable
import kotlin.jvm.JvmStatic

@Parcelize
class CreatorBeforeCompanions(val firstName: String) : Parcelable {
    companion {
        val test2 = parcelableCreator<CreatorBeforeCompanions>()
    }

    companion object {
        @JvmStatic
        val test = parcelableCreator<CreatorBeforeCompanions>()
    }
}

fun box(): String {
    if (CreatorBeforeCompanions.test !== parcelableCreator<CreatorBeforeCompanions>()) return "fail: companion object"
    if (CreatorBeforeCompanions.test2 !== parcelableCreator<CreatorBeforeCompanions>()) return "fail: companion block"
    return "OK"
}
