/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeRuntimeClasspathProvider.Companion.JUNIT_GENERATED_TEST_CLASS_FQNAME
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.JvmBoxMainClassProvider
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.runner.JUnitCore

class ParcelizeMainClassProvider(testServices: TestServices) : JvmBoxMainClassProvider(testServices) {
    override fun getMainClassNameAndAdditionalArguments(): List<String> {
        val robolectricProperties = System.getProperties().propertyNames().asSequence()
            .map { it.toString() }.filter { it.startsWith("robolectric") }
            .map { "-D$it=${System.getProperty(it)}" }
            .toList()
            .toTypedArray()

        return listOfNotNull(
            *robolectricProperties,
            JUnitCore::class.java.name,
            JUNIT_GENERATED_TEST_CLASS_FQNAME
        )
    }
}
