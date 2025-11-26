/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Attempts to limit diagnostic test data to only those compatible running under REPL.
 */
class ReplCompatDiagnosticTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val structure = testServices.moduleStructure
        val modules = structure.modules
        return modules.size > 1 || modules[0].files.size > 1 ||
                structure.allDirectives.contains(JvmEnvironmentConfigurationDirectives.JDK_KIND)
        // || contains package declaration

        // Too limited
        //        return testServices.moduleStructure.modules.singleOrNull()
        //            ?.files?.singleOrNull()
        //            ?.name?.endsWith(".kts") != true
    }
}
