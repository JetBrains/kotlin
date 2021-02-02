/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3

import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.kapt3.util.replaceAnonymousTypeWithSuperType
import org.jetbrains.kotlin.resolve.DeclarationSignatureAnonymousTypeTransformer
import org.jetbrains.kotlin.resolve.DescriptorUtils.isLocal
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.types.KotlinType

class KaptAnonymousTypeTransformer(
    private val analysisExtension: PartialAnalysisHandlerExtension
) : DeclarationSignatureAnonymousTypeTransformer {
    override fun transformAnonymousType(descriptor: DeclarationDescriptorWithVisibility, type: KotlinType): KotlinType? {
        if (!analysisExtension.analyzePartially) {
            return null
        }

        if (isLocal(descriptor)) {
            return type
        }

        return replaceAnonymousTypeWithSuperType(type)
    }
}