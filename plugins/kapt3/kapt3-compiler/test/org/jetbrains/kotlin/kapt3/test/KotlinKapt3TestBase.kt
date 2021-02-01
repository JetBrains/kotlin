/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.codegen.CodegenTestCase
import java.io.File

abstract class KotlinKapt3TestBase : CodegenTestCase() {
    val kaptFlagsToAdd = mutableListOf<KaptFlag>()
    val kaptFlagsToRemove = mutableListOf<KaptFlag>()

    override fun setUp() {
        super.setUp()
        kaptFlagsToAdd.clear()
        kaptFlagsToRemove.clear()
    }

    protected fun isFlagEnabled(flagName: String, testFile: File): Boolean {
        val stringToCheck = "// $flagName"
        return testFile.useLines { lines -> lines.any { it.trim() == stringToCheck } }
    }

    private fun isFlagDisabled(flagName: String, testFile: File): Boolean {
        val stringToCheck = "// !$flagName"
        return testFile.useLines { lines -> lines.any { it.trim() == stringToCheck } }
    }

    protected fun addOrRemoveFlag(flag: KaptFlag, testFile: File) {
        if (isFlagEnabled(flag.name, testFile)) {
            kaptFlagsToAdd.add(flag)
        } else if (isFlagDisabled(flag.name, testFile)) {
            kaptFlagsToRemove.add(flag)
        }
    }

    override fun doTest(filePath: String) {
        val testFile = File(filePath)

        kaptFlagsToAdd.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)

        addOrRemoveFlag(KaptFlag.CORRECT_ERROR_TYPES, testFile)
        addOrRemoveFlag(KaptFlag.STRICT, testFile)
        addOrRemoveFlag(KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES, testFile)

        super.doTest(filePath)
    }
}