/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.parcelize.test.services.ParcelizeRuntimeClasspathProvider.Companion.JUNIT_GENERATED_TEST_CLASS_FQNAME
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.jvm.JvmBoxMainClassProvider
import org.junit.runner.JUnitCore

class ParcelizeMainClassProvider(testServices: TestServices) : JvmBoxMainClassProvider(testServices) {
    override fun getMainClassNameAndAdditionalArguments(): List<String> {
        return listOfNotNull(
            "-Drobolectric.dependency.repo.url=https://cache-redirector.jetbrains.com/maven-central"
                .takeIf { "true" == System.getProperty("cacheRedirectorEnabled") },
            JUnitCore::class.java.name,
            JUNIT_GENERATED_TEST_CLASS_FQNAME
        )
    }
}
