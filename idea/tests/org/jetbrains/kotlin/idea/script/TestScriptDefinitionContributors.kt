/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import java.io.File
import kotlin.script.dependencies.Environment


class CustomScriptTemplateProvider(
        val environment: Environment
) : ScriptDefinitionContributor {
    override val id = "Test"

    override fun getDefinitions() = loadDefinitionsFromTemplates(
            templateClassNames = environment["template-classes-names"] as List<String>,
            templateClasspath = listOfNotNull(environment["template-classes"] as? File),
            environment = environment
    )

}

class FromTextTemplateProvider(
        val environment: Map<String, Any?>
) : ScriptDefinitionContributor {
    override val id = "Test"
    override fun getDefinitions() = loadDefinitionsFromTemplates(
            templateClassNames = listOf("org.jetbrains.kotlin.idea.script.Template"),
            templateClasspath = listOfNotNull(environment["template-classes"] as? File),
            environment = environment
    )
}