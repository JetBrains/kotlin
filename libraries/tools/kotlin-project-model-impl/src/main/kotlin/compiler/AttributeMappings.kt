/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.compiler

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.project.modelx.Attribute
import org.jetbrains.kotlin.project.modelx.JvmTargetAttribute
import org.jetbrains.kotlin.project.modelx.Platforms

/**
 * Unfortunately, at this moment there is only way to call compiler is to use K2*CompilerArguments
 * as input. Therefore, a lot of primitive mappings.
 */

internal fun K2JSCompilerArguments.applyKotlinAttribute(key: Attribute.Key, value: Attribute) {
    when (value) {
        is JvmTargetAttribute -> Unit
        is Platforms -> Unit
    }
}

internal fun K2JVMCompilerArguments.applyKotlinAttribute(key: Attribute.Key, value: Attribute) {
    when (value) {
        is JvmTargetAttribute -> jvmTarget = value.value.description
        is Platforms -> Unit
    }
}