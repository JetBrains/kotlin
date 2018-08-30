/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.TargetPlatformVersion

abstract class IdePlatform<Kind : IdePlatformKind<Kind>, Arguments : CommonCompilerArguments> {
    abstract val kind: Kind
    abstract val version: TargetPlatformVersion

    abstract fun createArguments(init: Arguments.() -> Unit = {}): Arguments

    val description
        get() = kind.name + " " + version.description

    override fun toString() = description
}
