/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.kotlin.annotation.plugin.ide.isAnnotated
import org.jetbrains.kotlin.annotation.plugin.ide.isOrdinaryClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.LightClassApplicabilityCheckExtension
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations.serializableAnnotationFqName
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType.LightClass
import org.jetbrains.kotlin.extensions.LightClassApplicabilityType.UltraLightClass

class IdeSerializationPluginApplicabilityExtension : LightClassApplicabilityCheckExtension {
    override fun checkApplicabilityType(declaration: KtDeclaration, descriptor: Lazy<DeclarationDescriptor?>): LightClassApplicabilityType {

        if (!declaration.isOrdinaryClass || !declaration.isAnnotated) return UltraLightClass

        return (descriptor.value as? ClassDescriptor)?.let {
            getIfEnabledOn(it) {
                if (it.annotations.hasAnnotation(serializableAnnotationFqName)) LightClass
                else UltraLightClass
            }
        } ?: UltraLightClass
    }
}