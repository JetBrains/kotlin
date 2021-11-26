/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object ParcelizeNames {
    // -------------------- Packages --------------------

    val DEPRECATED_RUNTIME_PACKAGE = FqName("kotlinx.android.parcel")

    private val PACKAGES_FQ_NAMES = listOf(
        FqName("kotlinx.parcelize"),
        DEPRECATED_RUNTIME_PACKAGE
    )

    // -------------------- Class ids --------------------

    val PARCELIZE_ID = ClassId(FqName("kotlinx.parcelize"), Name.identifier("Parcelize"))
    val OLD_PARCELIZE_ID = ClassId(FqName("kotlinx.android.parcel"), Name.identifier("Parcelize"))
    val PARCEL_ID = ClassId(FqName("android.os"), Name.identifier("Parcel"))
    val PARCELABLE_ID = ClassId(FqName("android.os"), Name.identifier("Parcelable"))
    val CREATOR_ID = PARCELABLE_ID.createNestedClassId(Name.identifier("Creator"))
    val PARCELER_ID = ClassId(FqName("kotlinx.parcelize"), Name.identifier("Parceler"))
    val OLD_PARCELER_ID = ClassId(FqName("kotlinx.android.parcel"), Name.identifier("Parceler"))

    val TYPE_PARCELER_CLASS_IDS = createClassIds("TypeParceler")
    val WRITE_WITH_CLASS_IDS = createClassIds("WriteWith")
    val IGNORED_ON_PARCEL_CLASS_IDS = createClassIds("IgnoredOnParcel")
    val PARCELIZE_CLASS_CLASS_IDS = createClassIds("Parcelize")
    val RAW_VALUE_ANNOTATION_CLASS_IDS = createClassIds("RawValue")

    // -------------------- FQNs --------------------

    val PARCELIZE_FQN = PARCELIZE_ID.asSingleFqName()
    val OLD_PARCELIZE_FQN = OLD_PARCELIZE_ID.asSingleFqName()
    val PARCELABLE_FQN = PARCELABLE_ID.asSingleFqName()
    val CREATOR_FQN = CREATOR_ID.asSingleFqName()

    val TYPE_PARCELER_FQ_NAMES = TYPE_PARCELER_CLASS_IDS.fqNames()
    val WRITE_WITH_FQ_NAMES = WRITE_WITH_CLASS_IDS.fqNames()
    val IGNORED_ON_PARCEL_FQ_NAMES = IGNORED_ON_PARCEL_CLASS_IDS.fqNames()
    val PARCELIZE_CLASS_FQ_NAMES: List<FqName> = PARCELIZE_CLASS_CLASS_IDS.fqNames()
    val RAW_VALUE_ANNOTATION_FQ_NAMES = RAW_VALUE_ANNOTATION_CLASS_IDS.fqNames()

    val PARCELER_FQN = PARCELER_ID.asSingleFqName()
    val OLD_PARCELER_FQN = OLD_PARCELER_ID.asSingleFqName()

    // -------------------- Names --------------------

    val DESCRIBE_CONTENTS_NAME = Name.identifier("describeContents")
    val WRITE_TO_PARCEL_NAME = Name.identifier("writeToParcel")
    val NEW_ARRAY_NAME = Name.identifier("newArray")
    val CREATE_FROM_PARCEL_NAME = Name.identifier("createFromParcel")

    val DEST_NAME = Name.identifier("dest")
    val FLAGS_NAME = Name.identifier("flags")

    val CREATOR_NAME = Name.identifier("CREATOR")

    // -------------------- Utils --------------------

    private fun createClassIds(name: String): List<ClassId> {
        return PACKAGES_FQ_NAMES.map { ClassId(it, Name.identifier(name)) }
    }

    private fun List<ClassId>.fqNames(): List<FqName> {
        return map { it.asSingleFqName() }
    }
}
