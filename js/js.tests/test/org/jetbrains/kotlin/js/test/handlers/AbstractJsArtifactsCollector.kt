/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsArtifactsCollector(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Js>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        if (module.name.endsWith(JsEnvironmentConfigurator.OLD_MODULE_SUFFIX)) return
        modulesToArtifact[module] = info.unwrap()
    }
}