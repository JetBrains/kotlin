/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.resolve.*

object NativePlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = NativePlatformConfigurator
    override val defaultImportsProvider: DefaultImportsProvider = NativeDefaultImportsProvider
}
