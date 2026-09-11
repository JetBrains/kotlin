/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.analysis

import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.dokka.analysis.kotlin.internal.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator

/**
 * Services exposed in [KotlinAnalysisPlugin] that are ready to be used.
 *
 * This class exists purely for convenience and to reduce boilerplate in tests.
 * It is analogous to calling `context.plugin<KotlinAnalysisPlugin>().querySingle { serviceName }`.
 */
class TestAnalysisServices(
    val sampleAnalysisEnvironmentCreator: SampleAnalysisEnvironmentCreator,
    val externalDocumentableProvider: ExternalDocumentableProvider,
    val moduleAndPackageDocumentationReader: ModuleAndPackageDocumentationReader
)
