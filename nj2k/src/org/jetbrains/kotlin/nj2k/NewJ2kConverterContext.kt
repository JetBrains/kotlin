/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.command.impl.DummyProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.*

data class NewJ2kConverterContext(
    val symbolProvider: JKSymbolProvider,
    val converter: NewJavaToKotlinConverter,
    val inConversionContext: (PsiElement) -> Boolean,
    val importStorage: ImportStorage,
    val elementsInfoStorage: JKElementInfoStorage
) : ConverterContext {
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

    companion object {
        val DUMMY =
            NewJ2kConverterContext(
                JKSymbolProvider(),
                NewJavaToKotlinConverter(
                    DummyProject.getInstance(),
                    ConverterSettings.defaultSettings,
                    EmptyJavaToKotlinServices
                ),
                { false },
                ImportStorage(),
                JKElementInfoStorage()
            )
    }

}

