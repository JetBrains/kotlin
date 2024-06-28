/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getObjCDocumentedAnnotations
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class GetObjCDocumentedAnnotationsTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - no annotation present`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            val objCDocumentedAnnotations = foo.getObjCDocumentedAnnotations()
            if (objCDocumentedAnnotations.isNotEmpty())
                fail("Expected no 'ObjC Documented Annotation present, Found. $objCDocumentedAnnotations")
        }
    }

    @Test
    fun `test - simple Important annotation`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            @MustBeDocumented
            annotation class ImportantAnnotation           
            annotation class OtherAnnotation
    
            @ImportantAnnotation @OtherAnnotation
            class Foo
        """.trimIndent()
        )

        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            val objCDocumentedAnnotations = foo.getObjCDocumentedAnnotations()
            if (objCDocumentedAnnotations.size != 1)
                fail("Expected single documented annotation. Found: $objCDocumentedAnnotations")

            val objCDocumentedAnnotation = objCDocumentedAnnotations.single()
            assertEquals("ImportantAnnotation", objCDocumentedAnnotation.classId?.shortClassName?.asString())
        }
    }

    @Test
    fun `test - special ObjC annotations are not documented for export`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                @MustBeDocumented
                annotation class ImportantAnnotation                     

                @kotlin.native.ObjCName("ObjCFoo")
                @kotlin.native.ShouldRefineInSwift
                @Deprecated("Deprecations shall also not be shown")
                @ImportantAnnotation
                class Foo
            """.trimIndent()
        )

        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            val objCDocumentedAnnotations = foo.getObjCDocumentedAnnotations()
            if (objCDocumentedAnnotations.size != 1)
                fail("Expected single documented annotation. Found: $objCDocumentedAnnotations")

            val objCDocumentedAnnotation = objCDocumentedAnnotations.single()
            assertEquals("ImportantAnnotation", objCDocumentedAnnotation.classId?.shortClassName?.asString())
        }
    }
}