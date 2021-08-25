/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.compiler

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.project.modelx.JSLanguageSetting
import org.jetbrains.kotlin.project.modelx.Jsr305Setting
import org.jetbrains.kotlin.project.modelx.JvmLanguageSetting
import org.jetbrains.kotlin.project.modelx.JvmStringConcatSetting

internal fun K2JSCompilerArguments.applyLanguageSetting(key: String, setting: JSLanguageSetting) {
    // TODO
}

internal fun K2JVMCompilerArguments.applyLanguageSetting(key: String, setting: JvmLanguageSetting) {
    when(setting) {
        is Jsr305Setting -> jsr305 = setting.value.description
        is JvmStringConcatSetting -> stringConcat = setting.value.description
    }
}