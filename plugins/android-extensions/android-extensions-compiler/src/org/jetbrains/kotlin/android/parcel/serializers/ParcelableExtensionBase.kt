/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel.serializers

import kotlinx.android.parcel.TypeParceler
import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent
import org.jetbrains.kotlin.android.parcel.isParcelize
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent.ComponentKind.*
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import java.io.FileDescriptor

interface ParcelableExtensionBase {

    companion object {
        val FILE_DESCRIPTOR_FQNAME = FqName(FileDescriptor::class.java.canonicalName)

        val CREATOR_NAME = Name.identifier("CREATOR")

        val ALLOWED_CLASS_KINDS = listOf(ClassKind.CLASS, ClassKind.OBJECT, ClassKind.ENUM_CLASS)
    }

    fun ClassDescriptor.hasCreatorField(): Boolean {
        val companionObject = companionObjectDescriptor ?: return false

        if (companionObject.name == CREATOR_NAME) {
            return true
        }

        return companionObject.unsubstitutedMemberScope
            .getContributedVariables(CREATOR_NAME, NoLookupLocation.FROM_BACKEND)
            .isNotEmpty()
    }

    val ClassDescriptor.isParcelableClassDescriptor get() = kind in ALLOWED_CLASS_KINDS && isParcelize

    fun ClassDescriptor.hasSyntheticDescribeContents() = hasParcelizeSyntheticMethod(DESCRIBE_CONTENTS)

    fun ClassDescriptor.hasSyntheticWriteToParcel() = hasParcelizeSyntheticMethod(WRITE_TO_PARCEL)

    fun ClassDescriptor.findFunction(componentKind: ParcelableSyntheticComponent.ComponentKind): SimpleFunctionDescriptor? {
        return unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier(componentKind.methodName), NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
            .firstOrNull { (it as? ParcelableSyntheticComponent)?.componentKind == componentKind }
    }

    private fun ClassDescriptor.hasParcelizeSyntheticMethod(componentKind: ParcelableSyntheticComponent.ComponentKind): Boolean {
        val methodName = Name.identifier(componentKind.methodName)

        val writeToParcelMethods = unsubstitutedMemberScope
            .getContributedFunctions(methodName, NoLookupLocation.FROM_BACKEND)
            .filter { it is ParcelableSyntheticComponent && it.componentKind == componentKind }

        return writeToParcelMethods.size == 1
    }
}