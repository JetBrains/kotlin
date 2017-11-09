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
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import java.io.File
import kotlin.script.extensions.SamWithReceiverAnnotations
import kotlin.script.templates.ScriptTemplateDefinition

abstract class AbstractSamWithReceiverScriptTest : AbstractDiagnosticsTest() {
    private companion object {
        private val TEST_ANNOTATIONS = emptyList<String>()
    }

    override fun createEnvironment(file: File) = super.createEnvironment(file).apply {
        StorageComponentContainerContributor.registerExtension(project, CliSamWithReceiverComponentContributor(TEST_ANNOTATIONS))
        val def = KotlinScriptDefinitionFromAnnotatedTemplate(ScriptForSamWithReceivers::class, null, null, emptyMap())
        configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, def)
    }
}

@ScriptTemplateDefinition
@SamWithReceiverAnnotations("SamWithReceiver1", "SamWithReceiver2")
abstract class ScriptForSamWithReceivers
