/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.handlers

import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractKaptHandler(testServices: TestServices) : AnalysisHandler<KaptContextBinaryArtifact>(
    testServices
) {
    override val failureDisablesNextSteps: Boolean = true
    override val doNotRunIfThereWerePreviousFailures: Boolean = true
    override val artifactKind: TestArtifactKind<KaptContextBinaryArtifact>
        get() = KaptContextBinaryArtifact.Kind
}
