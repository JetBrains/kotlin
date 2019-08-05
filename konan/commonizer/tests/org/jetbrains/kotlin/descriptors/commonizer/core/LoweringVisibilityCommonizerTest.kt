/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.descriptors.Visibility
import org.junit.Test

class LoweringVisibilityCommonizerTest : AbstractCommonizerTest<Visibility, Visibility>() {

    @Test
    fun publicOnly() = doTestSuccess(PUBLIC, PUBLIC, PUBLIC, PUBLIC)

    @Test
    fun protectedOnly() = doTestSuccess(PROTECTED, PROTECTED, PROTECTED, PROTECTED)

    @Test
    fun internalOnly() = doTestSuccess(INTERNAL, INTERNAL, INTERNAL, INTERNAL)

    @Test(expected = IllegalStateException::class)
    fun privateOnly() = doTestFailure(PRIVATE)

    @Test
    fun publicAndProtected() = doTestSuccess(PROTECTED, PUBLIC, PROTECTED, PUBLIC)

    @Test
    fun publicAndInternal() = doTestSuccess(INTERNAL, PUBLIC, INTERNAL, PUBLIC)

    @Test(expected = IllegalStateException::class)
    fun protectedAndInternal() = doTestFailure(PUBLIC, INTERNAL, PROTECTED)

    @Test(expected = IllegalStateException::class)
    fun publicAndPrivate() = doTestFailure(PUBLIC, INTERNAL, PRIVATE)

    @Test(expected = IllegalStateException::class)
    fun somethingUnexpected() = doTestFailure(PUBLIC, LOCAL)

    override fun createCommonizer() = VisibilityCommonizer.lowering()
}
