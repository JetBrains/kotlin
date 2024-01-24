/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.analysis

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.TestProject
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Context and data gathered during the analysis of a [TestProject].
 */
class TestAnalysisContext(

    /**
     * The actual [DokkaContext] that was used to run Dokka.
     *
     * Includes all plugins and classes available on classpath during the analysis.
     */
    val context: DokkaContext,

    /**
     * The actual [DokkaConfiguration] that was used to run Dokka.
     *
     * It was initially mapped from [TestDokkaConfiguration], and then added to by Dokka itself.
     */
    val configuration: DokkaConfiguration,

    /**
     * The entry point to the documentable model of the analyzed [TestProject].
     */
    val module: DModule
)
