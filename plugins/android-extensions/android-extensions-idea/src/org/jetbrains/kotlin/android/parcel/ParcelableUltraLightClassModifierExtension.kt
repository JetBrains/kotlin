/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import com.intellij.psi.impl.light.LightFieldBuilder
import org.jetbrains.kotlin.android.parcel.serializers.ParcelableExtensionBase
import org.jetbrains.kotlin.asJava.UltraLightClassModifierExtension
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.createGeneratedMethodFromDescriptor
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.util.isAnnotated
import org.jetbrains.kotlin.util.isOrdinaryClass

class ParcelableUltraLightClassModifierExtension : ParcelableExtensionBase, UltraLightClassModifierExtension {

    private fun tryGetParcelableClass(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>
    ): ClassDescriptor? {

        if (!declaration.isOrdinaryClass || !declaration.isAnnotated) return null

        val descriptorValue = descriptor.value ?: return null

        val parcelableClass = (descriptorValue as? ClassDescriptor)
            ?: descriptorValue.containingDeclaration as? ClassDescriptor
            ?: return null

        if (!parcelableClass.isParcelableClassDescriptor) return null

        return parcelableClass
    }

    override fun interceptFieldsBuilding(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>,
        containingDeclaration: KtUltraLightClass,
        fieldsList: MutableList<KtLightField>
    ) {

        val parcelableClass = tryGetParcelableClass(
            declaration = declaration,
            descriptor = descriptor
        ) ?: return

        if (parcelableClass.hasCreatorField()) return

        val fieldWrapper = KtLightFieldImpl.KtLightFieldForSourceDeclaration(
            origin = null,
            computeDelegate = {
                LightFieldBuilder("CREATOR", "android.os.Parcelable.Creator", containingDeclaration).also {
                    it.setModifiers("public", "static", "final")
                }
            },
            containingClass = containingDeclaration,
            dummyDelegate = null
        )

        fieldsList.add(fieldWrapper)
    }

    override fun interceptMethodsBuilding(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>,
        containingDeclaration: KtUltraLightClass,
        methodsList: MutableList<KtLightMethod>
    ) {

        val parcelableClass = tryGetParcelableClass(
            declaration = declaration,
            descriptor = descriptor
        ) ?: return

        with(parcelableClass) {
            if (hasSyntheticDescribeContents()) {
                findFunction(ParcelableSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS)?.let {
                    methodsList.add(
                        containingDeclaration.createGeneratedMethodFromDescriptor(it)
                    )
                }
            }

            if (hasSyntheticWriteToParcel()) {
                findFunction(ParcelableSyntheticComponent.ComponentKind.WRITE_TO_PARCEL)?.let {
                    methodsList.add(
                        containingDeclaration.createGeneratedMethodFromDescriptor(it)
                    )
                }
            }
        }
    }

}