/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.junit.Test

class KotlinUastResolveEverythingTest : AbstractKotlinResolveEverythingTest() {

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
    fun testTypeReferences() = doTest("TypeReferences")

}