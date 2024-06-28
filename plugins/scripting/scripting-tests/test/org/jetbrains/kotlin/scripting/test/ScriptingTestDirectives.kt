/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ScriptingTestDirectives : SimpleDirectivesContainer() {
    val SCRIPT_DEFAULT_IMPORTS by stringDirective("Default imports", multiLine = true)
    val SCRIPT_PROVIDED_PROPERTIES by stringDirective("Provided properties", multiLine = true)
    val GRADLE_LIKE_SCRIPT by directive("Provided properties")

    val directivesToPassViaEnvironment =
        listOf(
            SCRIPT_DEFAULT_IMPORTS to "defaultImports",
            SCRIPT_PROVIDED_PROPERTIES to "providedProperties",
            GRADLE_LIKE_SCRIPT to "gradleLikeScript",
        )
}
