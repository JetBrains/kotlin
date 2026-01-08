/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import java.nio.file.Path

class JdkProvider {
    private val TestVersions.Java.systemPropertyName get() = "jdk${numericVersion}Home"
    private val TestVersions.Java.environmentVariableName get() = "JDK_${numericVersion}"

    fun jdkHome(version: TestVersions.Java): Path? {
        val systemProperty = version.systemPropertyName
        val environmentVariable = version.environmentVariableName
        val jdkHome = System.getProperty(systemProperty) ?: System.getenv(environmentVariable) ?: return null
        return Path.of(jdkHome)
    }
}