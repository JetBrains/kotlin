/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object Kapt4TestDirectives : SimpleDirectivesContainer() {
    val EXPECTED_ERROR_K2 by stringDirective("Expected K2-specific error", multiLine = true)
}