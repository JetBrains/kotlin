/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.codegen.CodegenTestCase
import java.io.File

abstract class KotlinKapt3TestBase : CodegenTestCase() {
    val kaptFlags = mutableListOf<KaptFlag>()

    override fun setUp() {
        super.setUp()
        kaptFlags.clear()
    }

    protected fun File.isOptionSet(name: String) = this.useLines { lines -> lines.any { it.trim() == "// $name" } }

    override fun doTest(filePath: String) {
        val wholeFile = File(filePath)

        kaptFlags.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)

        fun handleFlag(flag: KaptFlag) {
            if (wholeFile.isOptionSet(flag.name)) {
                kaptFlags.add(flag)
            }
        }

        handleFlag(KaptFlag.CORRECT_ERROR_TYPES)
        handleFlag(KaptFlag.STRICT)
        handleFlag(KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES)

        super.doTest(filePath)
    }
}