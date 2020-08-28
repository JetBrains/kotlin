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

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.extensions.SamWithReceiverAnnotations
import kotlin.script.templates.ScriptTemplateDefinition

abstract class AbstractSamWithReceiverScriptTest : AbstractDiagnosticsTest() {

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        val def = ScriptDefinition.FromLegacy(
            defaultJvmScriptingHostConfiguration,
            KotlinScriptDefinitionFromAnnotatedTemplate(ScriptForSamWithReceivers::class, emptyMap())
        )
        environment.configuration.add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, def)
        val anns = def.annotationsForSamWithReceivers
        StorageComponentContainerContributor.registerExtension(environment.project, CliSamWithReceiverComponentContributor(anns))
    }
}

@ScriptTemplateDefinition
@SamWithReceiverAnnotations("SamWithReceiver1", "SamWithReceiver2")
abstract class ScriptForSamWithReceivers
