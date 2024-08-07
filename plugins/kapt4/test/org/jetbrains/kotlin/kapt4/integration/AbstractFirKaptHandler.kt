/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4.integration

import org.jetbrains.kotlin.kapt4.FirKaptContextBinaryArtifact
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractFirKaptHandler(testServices: TestServices) : AnalysisHandler<FirKaptContextBinaryArtifact>(
    testServices,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true
) {
    override val artifactKind: TestArtifactKind<FirKaptContextBinaryArtifact>
        get() = FirKaptContextBinaryArtifact.Kind
}
