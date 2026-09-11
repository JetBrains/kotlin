/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.kapt.test.KaptTestDirectives.IGNORE_COLLECTION_LITERALS_RESOLUTION
import org.jetbrains.kotlin.test.model.TestFailureSuppressorBySingleDirective
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Suppresses failures of test data marked with [IGNORE_COLLECTION_LITERALS_RESOLUTION].
 *
 * Registered only by the runner which enables
 * [org.jetbrains.kotlin.config.LanguageFeature.CollectionLiteralsBasedAnnotationResolution],
 * so the very same test data stays fully checked by the other runners.
 */
class KaptCollectionLiteralsSuppressor(testServices: TestServices) : TestFailureSuppressorBySingleDirective(
    suppressDirective = IGNORE_COLLECTION_LITERALS_RESOLUTION,
    directivesContainer = KaptTestDirectives,
    testServices = testServices,
)
