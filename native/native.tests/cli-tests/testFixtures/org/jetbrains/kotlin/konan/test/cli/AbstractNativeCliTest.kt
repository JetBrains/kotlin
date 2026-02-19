/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.cli

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.copyNativeHomeProperty
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractNativeCliTest : AbstractCliTest() {
    protected fun doNativeTest(fileName: String) {
        if (isTestDisabled(fileName)) return

        copyNativeHomeProperty()
        doTest(fileName, K2Native())
    }

    private fun isTestDisabled(fileName: String): Boolean {
        val directivesFile = File(fileName.replaceFirst("\\.args$".toRegex(), ".dirs"))
        if (!directivesFile.exists()) return false

        val family = HostManager.host.family

        return "targetFamily=${family}" in InTextDirectivesUtils.findListWithPrefixes(directivesFile.readText(), "// DISABLE_NATIVE: ")
    }
}