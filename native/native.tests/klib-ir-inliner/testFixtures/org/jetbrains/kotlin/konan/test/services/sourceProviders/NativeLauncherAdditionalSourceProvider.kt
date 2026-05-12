/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.services.sourceProviders

import org.jetbrains.kotlin.konan.test.blackbox.support.util.generateBoxFunctionLauncher
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceProviders.AbstractLauncherAdditionalSourceProvider

class NativeLauncherAdditionalSourceProvider(testServices: TestServices) : AbstractLauncherAdditionalSourceProvider(testServices) {
    override fun generateLauncherContent(boxFqName: String, expectedResult: String): String =
        generateBoxFunctionLauncher(boxFqName, expectedResult)
}
