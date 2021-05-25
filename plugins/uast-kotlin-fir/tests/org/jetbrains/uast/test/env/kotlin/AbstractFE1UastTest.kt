/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.env.kotlin

import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.kotlin.AbstractKotlinUastTest
import java.io.File

abstract class AbstractFE1UastTest : AbstractKotlinUastTest() {
    override var testDataDir = File("plugins/uast-kotlin-fir/testData")

    fun doTest(filePath: String) {
        testDataDir = File(filePath).parentFile
        val testName = File(filePath).nameWithoutExtension
        val virtualFile = getVirtualFile(testName)

        val psiFile = psiManager.findFile(virtualFile) ?: error("Can't get psi file for $testName")
        val uFile = uastContext.convertElementWithParent(psiFile, null) ?: error("Can't get UFile for $testName")
        check(filePath, uFile as UFile)
    }
}
