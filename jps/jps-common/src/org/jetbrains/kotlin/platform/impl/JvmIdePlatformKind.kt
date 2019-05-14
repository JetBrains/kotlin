/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmIdePlatformUtil")

package org.jetbrains.kotlin.platform.impl

import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

object JvmIdePlatformKind : IdePlatformKind<JvmIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        if (arguments !is K2JVMCompilerArguments) return null

        val jvmTargetDescription = arguments.jvmTarget
            ?: return JvmPlatforms.defaultJvmPlatform

        val jvmTarget = JvmTarget.values()
            .firstOrNull { VersionComparatorUtil.COMPARATOR.compare(it.description, jvmTargetDescription) >= 0 }
            ?: return JvmPlatforms.defaultJvmPlatform

        return JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget)
    }

    override fun createArguments(): CommonCompilerArguments {
        return K2JVMCompilerArguments()
    }

    override val platforms: List<TargetPlatform> = JvmTarget.values().map { ver -> JvmPlatforms.jvmPlatformByTargetVersion(ver) } + listOf(JvmPlatforms.defaultJvmPlatform)
    override val defaultPlatform get() = JvmPlatforms.defaultJvmPlatform

    override val argumentsClass get() = K2JVMCompilerArguments::class.java

    override val name get() = "JVM"
}

val IdePlatformKind<*>?.isJvm
    get() = this is JvmIdePlatformKind
