/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.junit.Test
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class IrKotlinKapt3IntegrationTests : AbstractIrKotlinKapt3IntegrationTest(), CustomJdkTestLauncher {
    override fun test(
        name: String,
        vararg supportedAnnotations: String,
        options: Map<String, String>,
        process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit
    ) {
        super.test(name, *supportedAnnotations, options = options, process = process)

        doTestWithJdk11(
            SingleJUnitTestRunner::class.java,
            IrKotlinKapt3IntegrationTests::class.java.name + "#test" + getTestName(false)
        )
    }

    @Test
    fun testSimple() = test("Simple", "test.MyAnnotation") { set, roundEnv, _ ->
        assertEquals(1, set.size)
        val annotatedElements = roundEnv.getElementsAnnotatedWith(set.single())
        assertEquals(1, annotatedElements.size)
        assertEquals("myMethod", annotatedElements.single().simpleName.toString())
    }

}
