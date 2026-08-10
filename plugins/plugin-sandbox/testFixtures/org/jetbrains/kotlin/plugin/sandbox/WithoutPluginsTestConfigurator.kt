/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class WithoutPluginsTestConfigurator(testServices: TestServices): MetaTestConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(PluginSandboxDirectives)

    override fun shouldSkipTest() = PluginSandboxDirectives.WITH_AND_WITHOUT_PLUGIN !in testServices.moduleStructure.allDirectives
}
