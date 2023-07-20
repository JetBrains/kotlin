/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

class Kapt4EnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> = mapOf(
        JvmAnalysisFlags.generatePropertyAnnotationsMethods to true,
        JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.ALL_INCOMPATIBLE
    )

    override val directiveContainers: List<DirectivesContainer> = listOf(Kapt4TestDirectives)
}