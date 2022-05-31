/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class KotlinKapt3TestBase : CodegenTestCase() {
    val kaptFlagsToAdd = mutableListOf<KaptFlag>()
    val kaptFlagsToRemove = mutableListOf<KaptFlag>()
    private val directoriesToCleanup = mutableListOf<File>()

    override fun setUp() {
        super.setUp()
        kaptFlagsToAdd.clear()
        kaptFlagsToRemove.clear()
    }

    override fun tearDown() {
        directoriesToCleanup.forEach(File::deleteRecursively)
        super.tearDown()
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

    protected fun tmpDir(name: String): File {
        return KtTestUtil.tmpDir(name).also(directoriesToCleanup::add)
    }

    protected fun checkTxtAccordingToBackend(txtFile: File, actual: String) {
        val irTxtFile = File(txtFile.parentFile, txtFile.nameWithoutExtension + "_ir.txt")
        val expectedFile =
            if (backend.isIR && irTxtFile.exists()) irTxtFile
            else txtFile
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)

        if (backend.isIR && txtFile.exists() && irTxtFile.exists() && txtFile.readText() == irTxtFile.readText()) {
            fail("JVM and JVM_IR golden files are identical. Remove $irTxtFile.")
        }
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)

        if (backend.isIR) {
            configuration.put(JVMConfigurationKeys.IR, true)
            configuration.put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)
        }
    }

    override fun doTest(filePath: String) {
        val testFile = File(filePath)

        kaptFlagsToAdd.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)

        addOrRemoveFlag(KaptFlag.CORRECT_ERROR_TYPES, testFile)
        addOrRemoveFlag(KaptFlag.STRICT, testFile)
        addOrRemoveFlag(KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES, testFile)

        if (backend.isIR) {
            kaptFlagsToAdd.add(KaptFlag.USE_JVM_IR)
        }

        super.doTest(filePath)
    }
}
