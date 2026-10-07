/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

val ScriptingHostConfigurationKeys.overriddenHostProperty by PropertiesCollection.key<String>()
val ScriptingHostConfigurationKeys.templateOnlyHostProperty by PropertiesCollection.key<String>()

/**
 * Host configuration of a test script definition: [overriddenHostProperty] is expected to be overridden
 * by an explicit host configuration, while [templateOnlyHostProperty] is expected to be kept.
 */
class TestTemplateHostConfiguration : ScriptingHostConfiguration(
    body = {
        overriddenHostProperty("template")
        templateOnlyHostProperty("template-only")
    }
)

class CapturedHostProperties(
    var overriddenHostProperty: String? = null,
    var templateOnlyHostProperty: String? = null,
)

/**
 * Captures the host properties seen by the refinement into [captured].
 */
fun ScriptCompilationConfiguration.Builder.captureHostConfigurationOnRefinement(captured: CapturedHostProperties) {
    refineConfiguration {
        beforeParsing { context ->
            val hostConfiguration = context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]
            captured.overriddenHostProperty = hostConfiguration?.get(ScriptingHostConfiguration.overriddenHostProperty)
            captured.templateOnlyHostProperty = hostConfiguration?.get(ScriptingHostConfiguration.templateOnlyHostProperty)
            context.compilationConfiguration.asSuccess()
        }
    }
}
