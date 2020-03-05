/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Modality.*
import org.junit.Test

class DefaultModalityCommonizerTest : AbstractCommonizerTest<Modality, Modality>() {

    @Test
    fun onlyFinal() = doTestSuccess(FINAL, FINAL, FINAL, FINAL)

    @Test
    fun onlyOpen() = doTestSuccess(OPEN, OPEN, OPEN, OPEN)

    @Test
    fun onlySealed() = doTestSuccess(SEALED, SEALED, SEALED, SEALED)

    @Test
    fun onlyAbstract() = doTestSuccess(ABSTRACT, ABSTRACT, ABSTRACT, ABSTRACT)

    @Test(expected = IllegalCommonizerStateException::class)
    fun sealedAndAbstract() = doTestFailure(SEALED, ABSTRACT)

    @Test(expected = IllegalCommonizerStateException::class)
    fun sealedAndFinal() = doTestFailure(SEALED, FINAL)

    @Test(expected = IllegalCommonizerStateException::class)
    fun abstractAndFinal() = doTestFailure(ABSTRACT, FINAL)

    @Test
    fun finalAndOpen() = doTestSuccess(FINAL, FINAL, OPEN, FINAL)

    @Test
    fun openAndFinal() = doTestSuccess(FINAL, OPEN, OPEN, FINAL)

    override fun createCommonizer() = ModalityCommonizer.default()
}
