/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.junit.Test

class KotlinUastIdentifiersTest : AbstractKotlinIdentifiersTest() {

    @Test
    fun testClassAnnotation() = doTest("ClassAnnotation")

    @Test
    fun testLocalDeclarations() = doTest("LocalDeclarations")

    @Test
    fun testConstructors() = doTest("Constructors")

    @Test
    fun testSimpleAnnotated() = doTest("SimpleAnnotated")

    @Test
    fun testAnonymous() = doTest("Anonymous")

    @Test
    fun testLambdas() = doTest("Lambdas")

    @Test
    fun testSuperCalls() = doTest("SuperCalls")

    @Test
    fun testPropertyInitializer() = doTest("PropertyInitializer")

    @Test
    fun testEnumValuesConstructors() = doTest("EnumValuesConstructors")

}