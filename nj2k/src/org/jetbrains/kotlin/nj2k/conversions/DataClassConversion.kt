/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.primaryConstructor
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtPrimaryConstructorImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKMutabilityModifierElementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKOtherModifierElementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKParameterImpl

class DataClassConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)

        val lombokAnnotation = element.annotationList.annotations.firstOrNull { 
            val fqName = it.classSymbol.fqName
            fqName == "lombok.Value" || fqName == "lombok.Data" 
        } ?: return recurse(element)

        val properties = element.classBody.declarations.filterIsInstance<JKKtProperty>()
        if (properties.isEmpty()) return recurse(element)
        val primaryConstructor = element.primaryConstructor() as? JKKtPrimaryConstructorImpl ?: return recurse(element)

        val isValueAnnotation = lombokAnnotation.classSymbol.fqName == "lombok.Value"
        primaryConstructor.parameters = properties.map {
            it.invalidate()
            val mutabilityElement = if (isValueAnnotation) JKMutabilityModifierElementImpl(Mutability.IMMUTABLE) else it.mutabilityElement
            JKParameterImpl(it.type, it.name, false, it.initializer, it.annotationList, mutabilityElement)
        }
        element.classBody.declarations -= properties
        element.annotationList.annotations -= lombokAnnotation
        element.otherModifierElements += JKOtherModifierElementImpl(OtherModifier.DATA)

        return recurse(element)
    }
}