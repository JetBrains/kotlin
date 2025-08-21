/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.internal.jvm.JavaInfo
import org.gradle.internal.jvm.Jvm
import java.io.File

// Prop names are defined in 'build.gradle.kts'
private const val JDK_8_PROP_NAME = "jdk8Home"
private const val JDK_11_PROP_NAME = "jdk11Home"
private const val JDK_17_PROP_NAME = "jdk17Home"
private const val JDK_21_PROP_NAME = "jdk21Home"

internal val allJdkProperties = setOf(
    JDK_8_PROP_NAME,
    JDK_11_PROP_NAME,
    JDK_17_PROP_NAME,
    JDK_21_PROP_NAME
)

internal fun getUserJdk(): JavaInfo = Jvm.forHome(File(System.getProperty("java.home")))

internal val jdk8Info: JavaInfo = Jvm.forHome(File(System.getProperty(JDK_8_PROP_NAME)))
internal val jdk11Info: JavaInfo = Jvm.forHome(File(System.getProperty(JDK_11_PROP_NAME)))
internal val jdk17Info: JavaInfo = Jvm.forHome(File(System.getProperty(JDK_17_PROP_NAME)))
internal val jdk21Info: JavaInfo = Jvm.forHome(File(System.getProperty(JDK_21_PROP_NAME)))

// replace required for windows paths so Groovy will not complain about unexpected char '\'
internal val JavaInfo.jdkPath get() = javaHome.absolutePath.replace("\\", "\\\\")

internal val JavaInfo.jdkRealPath
    get() = javaHome
        .toPath()
        .toRealPath()
        .toAbsolutePath()
        .toString()
