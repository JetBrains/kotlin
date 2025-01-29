// CURIOUS_ABOUT: writeToParcel, createFromParcel
// WITH_STDLIB

@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

import kotlinx.parcelize.*
import android.os.Parcelable
import kotlin.uuid.Uuid

@Parcelize
data class Test(
    val uuidData: Uuid,
) : Parcelable
