/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

class TestDisplayNameGenerator : DisplayNameGenerator {
    private val tag = System.getProperty("testDisplayName.tag")
    private val default = DisplayNameGenerator.Standard()

    override fun generateDisplayNameForClass(testClass: Class<*>?): String {
        return default.generateDisplayNameForClass(testClass)
    }

    override fun generateDisplayNameForNestedClass(nestedClass: Class<*>?): String {
        return default.generateDisplayNameForNestedClass(nestedClass)
    }

    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method?): String {
        val defaultName = default.generateDisplayNameForMethod(testClass, testMethod)
        val isTodo = testMethod?.isAnnotationPresent(TodoAnalysisApi::class.java) ?: false && tag == "AA"

        return buildString {
            if (tag != null) append("[$tag] ")
            append(defaultName)
            if (isTodo) append(" // TODO")
        }
    }
}
