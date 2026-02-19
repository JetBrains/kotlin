/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object PowerAssertConfigurationDirectives : SimpleDirectivesContainer() {
    val FUNCTION by stringDirective(
        description = "Functions targeted by Power-Assert transformation",
        multiLine = true,
    )

    val WITH_JUNIT5 by directive("Add JUnit5 to classpath")
}
