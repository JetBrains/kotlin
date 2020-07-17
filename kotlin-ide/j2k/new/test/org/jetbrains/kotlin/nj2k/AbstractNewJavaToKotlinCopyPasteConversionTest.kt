/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.test.KotlinRoot

abstract class AbstractNewJavaToKotlinCopyPasteConversionTest : AbstractJavaToKotlinCopyPasteConversionTest() {
    override fun getTestDataDirectory() = KotlinRoot.DIR.resolve("j2k/new/testData/copyPaste")
    override fun isNewJ2K(): Boolean = true
}