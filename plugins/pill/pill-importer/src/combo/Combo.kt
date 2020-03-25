/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.pill.combo.intellij.IntellijComboGenerator
import org.jetbrains.kotlin.pill.combo.intellij.IntellijRepoGenerator
import java.io.File

enum class Combo {
    INTELLIJ {
        override fun createGenerator(kotlinProjectDir: File, logger: Logger): ComboGenerator {
            return IntellijComboGenerator(kotlinProjectDir)
        }

        override fun runTask(taskName: String, kotlinProjectDir: File, logger: Logger) {
            when (taskName) {
                "genRepo" -> IntellijRepoGenerator(kotlinProjectDir).generate()
                else -> error("IntelliJ combo: Unknown task name $taskName")
            }
        }
    };

    abstract fun createGenerator(kotlinProjectDir: File, logger: Logger): ComboGenerator
    abstract fun runTask(taskName: String, kotlinProjectDir: File, logger: Logger)
}