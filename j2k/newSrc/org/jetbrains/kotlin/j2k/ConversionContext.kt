/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.tree.JKElement

data class ConversionContext(
    val symbolProvider: JKSymbolProvider,
    val converter: NewJavaToKotlinConverter,
    val inConversionContext: (PsiElement) -> Boolean
) {
    val project: Project get() = converter.project
    val typeFlavorCalculator = TypeFlavorCalculator(object : TypeFlavorConverterFacade {
        override val referenceSearcher: ReferenceSearcher
            get() = converter.converterServices.oldServices.referenceSearcher
        override val javaDataFlowAnalyzerFacade: JavaDataFlowAnalyzerFacade
            get() = converter.converterServices.oldServices.javaDataFlowAnalyzerFacade
        override val resolverForConverter: ResolverForConverter
            get() = converter.converterServices.oldServices.resolverForConverter

        override fun inConversionScope(element: PsiElement): Boolean = inConversionContext(element)
    })
}