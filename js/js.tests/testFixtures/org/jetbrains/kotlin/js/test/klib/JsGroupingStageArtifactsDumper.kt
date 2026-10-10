/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.handlers.JsArtifactsDumpHandler
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Copies the JS output of grouped batch into the `js/js.tests/build/out` directories of the tests of the batch,
 * as [JsArtifactsDumpHandler.Checker] does for a test of the one-stage pipeline, so that JS stack traces are navigable
 * in the IDE. The checker itself runs at the end of the non-grouping stage, when no JS exists yet, so the grouping stage
 * needs a handler of its own for that.
 *
 * The executable of a grouped batch is produced into the artifacts directory of the first test of the batch, so it is
 * dumped as the JS of that test; an isolated test is the only test of its batch. The directories of the other tests
 * hold nothing, so they are not even looked at.
 */
class JsGroupingStageArtifactsDumper(testServices: TestServices) : GroupingStageHandler<BinaryArtifacts.Js>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    override fun processArtifact(artifact: BinaryArtifacts.Js) {
        JsArtifactsDumpHandler.Checker(testServices.groupingStageInputs.first().testServices).check(thereWereFailures = false)
    }
}
