/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/** Skips tests marked with [KaptTestDirectives.MIN_JAVA_VERSION] on an older JVM. */
class KaptMinJavaVersionSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(KaptTestDirectives)

    override fun shouldSkipTest(): Boolean {
        val minVersion = testServices.moduleStructure.allDirectives.singleOrZeroValue(KaptTestDirectives.MIN_JAVA_VERSION) ?: return false
        // "1.8" before Java 9, "9", "11", ... after
        val javaVersion = System.getProperty("java.specification.version").substringAfter("1.").toInt()
        return javaVersion < minVersion
    }
}
