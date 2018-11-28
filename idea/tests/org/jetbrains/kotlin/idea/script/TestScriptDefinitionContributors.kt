/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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