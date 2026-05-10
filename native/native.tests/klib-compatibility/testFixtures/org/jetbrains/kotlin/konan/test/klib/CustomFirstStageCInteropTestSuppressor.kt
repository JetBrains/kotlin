/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion.Companion.LATEST_STABLE
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.services.TestServices

/*
 * Suppresses errors of earlier versions of cinterop tool while compiling newer tests for newer platformlibs or using newer cli options
 */
class CustomFirstStageCInteropTestSuppressor(
    testServices: TestServices,
) : TestFailureSuppressor(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CustomKlibCompilerTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        return failedAssertions.filterNot {
            customNativeCompilerSettings.defaultLanguageVersion < LATEST_STABLE
                    && it is WrappedException.FromFacade
                    && it.facade is ObjCInteropFacade
        }
    }

    override fun checkIfTestShouldBeUnmuted() {}
}
