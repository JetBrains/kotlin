/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test

import org.jetbrains.kotlin.kapt.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt.base.util.WriterBackedKaptLogger
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

object JavaKaptContextUtils {
    val logger = WriterBackedKaptLogger(isVerbose = true)

    fun simpleProcessor() = IncrementalProcessor(
        object : AbstractProcessor() {
            override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                for (annotation in annotations) {
                    val annotationName = annotation.simpleName.toString()
                    val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)

                    for (annotatedElement in annotatedElements) {
                        val generatedClassName = annotatedElement.simpleName.toString().replaceFirstChar(Char::uppercaseChar) +
                                annotationName.replaceFirstChar(Char::uppercaseChar)
                        val file = processingEnv.filer.createSourceFile("generated.$generatedClassName")
                        file.openWriter().use {
                            it.write(
                                """
                            package generated;
                            class $generatedClassName {}
                            """.trimIndent()
                            )
                        }
                    }
                }

                return true
            }

            override fun getSupportedAnnotationTypes() = setOf("test.MyAnnotation")
        }, DeclaredProcType.NON_INCREMENTAL, logger
    )

}
