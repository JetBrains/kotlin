/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.extensions.SamWithReceiverAnnotations
import kotlin.script.templates.ScriptTemplateDefinition

@OptIn(ObsoleteTestInfrastructure::class)
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
