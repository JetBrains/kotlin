/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4.integration

import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.handlers.AbstractKaptHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class FirProcessorWasCalledHandler(testServices: TestServices) : AbstractKaptHandler(testServices) {
    override val artifactKind: TestArtifactKind<KaptContextBinaryArtifact>
        get() = KaptContextBinaryArtifact.Kind

    override fun processModule(module: TestModule, info: KaptContextBinaryArtifact) {
        assertions.assertTrue(testServices.firKaptExtensionProvider[module].started) { "Annotation processor was not started" }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
