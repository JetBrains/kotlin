/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.js.test.JsSteppingTestAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.handlers.JsDebugRunner
import org.jetbrains.kotlin.js.test.handlers.JsDtsHandler
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJsArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import java.lang.Boolean.getBoolean

fun TestConfigurationBuilder.configureLineNumberTests(createLineNumberHandler: (testServices: TestServices) -> JsBinaryArtifactHandler) {
    defaultDirectives {
        +JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME
        +JsEnvironmentConfigurationDirectives.NO_COMMON_FILES
        -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE.with(listOf("JS", "JS_IR", "JS_IR_ES6"))
    }
    configureJsArtifactsHandlersStep {
        useHandlers(createLineNumberHandler)
    }
}

fun TestConfigurationBuilder.configureSteppingTests() {
    defaultDirectives {
        +JsEnvironmentConfigurationDirectives.NO_COMMON_FILES
    }
    useAdditionalSourceProviders(::JsSteppingTestAdditionalSourceProvider)
    jsArtifactsHandlersStep {
        useHandlers(::JsDebugRunner.bind(false))
    }
}

fun TestConfigurationBuilder.configureJsTypeScriptExportTest() {
    defaultDirectives {
        +JsEnvironmentConfigurationDirectives.GENERATE_DTS
        if (getBoolean("kotlin.js.updateReferenceDtsFiles")) +JsEnvironmentConfigurationDirectives.UPDATE_REFERENCE_DTS_FILES
    }
    configureJsArtifactsHandlersStep {
        useHandlers(::JsDtsHandler)
    }
}
